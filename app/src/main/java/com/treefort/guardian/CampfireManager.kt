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
 * to every member listed in `room_joined`; members answer. Audio only, one shared mic track, no
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
            _state.value.peers.forEach { WebRTCManager.sendRaw(hangupFrame(it)) }
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
        if (!_state.value.inRoom) return false
        val (type, from) = CampfireSignaling.meshFrom(raw) ?: return false
        if (from !in _state.value.peers) {
            // A stranger's 1:1 call while we're in the Campfire: decline it (we are busy).
            if (type == "call_offer") WebRTCManager.sendRaw(
                JSONObject().put("type", "call_rejected").put("target_dial_code", from).put("reason", "In the Campfire").toString(),
            )
            return type == "call_offer" || type == "ice_candidate"
        }
        val json = JSONObject(raw)
        when (type) {
            "call_offer" -> answer(from, json.optString("sdp"))
            "call_answer" -> pcs[from]?.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, json.optString("sdp")))
            "ice_candidate" -> json.optJSONObject("candidate")?.let {
                pcs[from]?.addIceCandidate(IceCandidate(it.optString("sdpMid"), it.optInt("sdpMLineIndex"), it.optString("candidate")))
            }
            "call_hangup" -> closePeer(from)
            else -> return false
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
                val cand = JSONObject().put("candidate", c.sdp).put("sdpMid", c.sdpMid).put("sdpMLineIndex", c.sdpMLineIndex)
                WebRTCManager.sendRaw(JSONObject().put("type", "ice_candidate").put("target_dial_code", peer).put("candidate", cand).toString())
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
                        WebRTCManager.sendRaw(
                            JSONObject().put("type", "call_offer").put("target_dial_code", peer)
                                .put("room_code", _state.value.roomCode).put("caller_name", WebRTCManager.callerName)
                                .put("sdp", desc.description).toString(),
                        )
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
                                WebRTCManager.sendRaw(
                                    JSONObject().put("type", "call_answer").put("target_dial_code", peer)
                                        .put("sdp", desc.description).toString(),
                                )
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

    private fun hangupFrame(peer: String) = JSONObject().put("type", "call_hangup").put("target_dial_code", peer).toString()

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
