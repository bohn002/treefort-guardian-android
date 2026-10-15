package com.treefort.guardian

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

/**
 * Campfire group audio: one audio-only mesh over the existing signaling socket. The joiner offers
 * to every member listed in `room_joined`; members answer. Legs use `room_offer` / `room_answer` /
 * `room_ice` / `room_leave` (RISK-04, docs/SIGNALING.md); the hub relays them only between members
 * of the same room. Audio only, one shared mic track, no
 * video, no recording, so memory stays at one PeerConnection per other participant.
 *
 * Only household owners and guardians may join (callers get `call_rejected` from the backend, and
 * the UI never offers the control). Signaling frames reach us from [WebRTCManager] via [handle].
 */
object CampfireManager {
    private const val TAG = "TreeFort-Campfire"

    private val _state = MutableStateFlow(CampfireState())
    val state: StateFlow<CampfireState> = _state.asStateFlow()
    val isMuted = MutableStateFlow(false)

    private val pcs = HashMap<String, PeerConnection>()
    private var source: AudioSource? = null
    private var track: AudioTrack? = null

    /** Joins the household's Campfire. False if a 1:1 call is live or signaling is not ready. */
    fun join(householdId: Int?): Boolean {
        if (_state.value.inRoom || _state.value.joining) return true
        if (WebRTCManager.callState.value !is CallState.Idle || !WebRTCManager.isOnline.value) return false
        if (WebRTCManager.peerFactory() == null) return false
        _state.value = CampfireState(joining = true)
        WebRTCManager.sendRaw(CampfireSignaling.join(CampfireSignaling.DEFAULT_ROOM, householdId))
        return true
    }

    fun leave() {
        val room = _state.value.roomCode
        if (room != null) {
            _state.value.peers.forEach { WebRTCManager.sendRaw(CampfireSignaling.leaveLeg(it, room)) }
            WebRTCManager.sendRaw(CampfireSignaling.leave(room))
        }
        teardown(null)
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        track?.setEnabled(!isMuted.value)
    }

    fun clearNotice() { _state.value = _state.value.copy(notice = null) }

    /** Socket dropped: the server already removed us from the room. */
    fun onSignalingLost() {
        if (_state.value.inRoom || _state.value.joining) teardown("Connection lost. Rejoin the Campfire.")
    }

    /** True if [raw] belonged to the Campfire and must not reach the 1:1 call handler. */
    fun handle(raw: String): Boolean {
        CampfireSignaling.parse(raw)?.let { e ->
            val before = _state.value
            // A "can't join" call_rejected is only ours while a join is pending.
            if (e is RoomEvent.Rejected && !before.joining) return false
            _state.value = before.reduce(e, WebRTCManager.myDialCode)
            when (e) {
                is RoomEvent.Joined -> { startLocalAudio(); e.peers.filter { it != WebRTCManager.myDialCode }.forEach(::offerTo) }
                is RoomEvent.PeerLeft -> closePeer(e.peer)
                is RoomEvent.Closed -> if (e.roomCode == before.roomCode) teardown(e.reason ?: "The Campfire closed.")
                is RoomEvent.Rejected -> teardown(e.reason)
                is RoomEvent.PeerJoined -> Unit // they will offer to us
            }
            return true
        }
        when (val a = CampfireSignaling.meshAction(raw, _state.value)) {
            MeshAction.NotOurs -> return false
            MeshAction.Swallow -> Unit
            // A 1:1 call while we're in the Campfire: decline it (we are busy).
            is MeshAction.DeclineCall -> WebRTCManager.sendRaw(
                JSONObject().put("type", "call_rejected").put("target_dial_code", a.peer).put("reason", "In the Campfire").toString(),
            )
            is MeshAction.RefuseLeg -> WebRTCManager.sendRaw(CampfireSignaling.leaveLeg(a.peer, _state.value.roomCode, "Not in this Campfire"))
            is MeshAction.Answer -> answer(a.peer, a.sdp)
            is MeshAction.RemoteAnswer ->
                pcs[a.peer]?.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, a.sdp))
            is MeshAction.Ice -> pcs[a.peer]?.addIceCandidate(IceCandidate(a.sdpMid, a.sdpMLineIndex, a.candidate))
            is MeshAction.CloseLeg -> closePeer(a.peer)
        }
        return true
    }

    // ---- mesh ---------------------------------------------------------------------------------

    private fun startLocalAudio() {
        if (track != null) return
        val f = WebRTCManager.peerFactory() ?: return
        source = f.createAudioSource(MediaConstraints())
        track = f.createAudioTrack("campfire_audio", source).also { it.setEnabled(!isMuted.value) }
    }

    private fun newPeer(peer: String): PeerConnection? {
        pcs[peer]?.let { return it }
        val f = WebRTCManager.peerFactory() ?: return null
        val cfg = PeerConnection.RTCConfiguration(WebRTCManager.iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val pc = f.createPeerConnection(cfg, object : PeerConnection.Observer {
            override fun onIceCandidate(c: IceCandidate?) {
                c ?: return
                WebRTCManager.sendRaw(CampfireSignaling.ice(peer, _state.value.roomCode, c.sdp, c.sdpMid, c.sdpMLineIndex))
            }
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                if (s == PeerConnection.IceConnectionState.FAILED) closePeer(peer)
            }
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(s: MediaStream?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(c: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: org.webrtc.RtpReceiver?, s: Array<out MediaStream>?) {}
        }) ?: return null
        track?.let { pc.addTrack(it, listOf("campfire")) }
        pcs[peer] = pc
        return pc
    }

    private fun offerTo(peer: String) {
        val pc = newPeer(peer) ?: return
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc ?: return
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        WebRTCManager.sendRaw(CampfireSignaling.offer(peer, _state.value.roomCode, desc.description))
                    }
                }, desc)
            }
        }, audioConstraints())
    }

    private fun answer(peer: String, sdp: String) {
        startLocalAudio()
        val pc = newPeer(peer) ?: return
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc ?: return
                        pc.setLocalDescription(object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                WebRTCManager.sendRaw(CampfireSignaling.answer(peer, _state.value.roomCode, desc.description))
                            }
                        }, desc)
                    }
                }, audioConstraints())
            }
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }

    private fun audioConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    private fun closePeer(peer: String) {
        pcs.remove(peer)?.close()
    }

    private fun teardown(notice: String?) {
        pcs.values.forEach { runCatching { it.close() } }
        pcs.clear()
        track?.setEnabled(false); track = null
        source?.dispose(); source = null
        isMuted.value = false
        _state.value = CampfireState(notice = notice)
        Log.i(TAG, "Campfire closed" + (notice?.let { ": $it" } ?: ""))
    }
}
