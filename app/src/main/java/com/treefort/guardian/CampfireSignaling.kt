package com.treefort.guardian

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Server-to-client Campfire room events (backend `signaling/webrtc.py`). Pure: no Android types. */
sealed interface RoomEvent {
    /** We are in; [peers] are the members already there and are the ones we offer to. */
    data class Joined(val roomCode: String, val peers: List<String>) : RoomEvent
    /** Someone entered after us; they will send us an offer. */
    data class PeerJoined(val roomCode: String, val peer: String) : RoomEvent
    data class PeerLeft(val roomCode: String, val peer: String) : RoomEvent
    /** The server removed us (membership revoked). */
    data class Closed(val roomCode: String, val reason: String?) : RoomEvent
    /** Join refused, e.g. a caller trying to enter a household's Campfire. */
    data class Rejected(val roomCode: String, val reason: String) : RoomEvent
}

/**
 * What the Campfire does with one mesh frame (RISK-04). Legs travel only as `room_offer` /
 * `room_answer` / `room_ice` / `room_leave`; plain `call_*` frames are never room legs.
 */
sealed interface MeshAction {
    /** Not a Campfire frame (or a 1:1 frame while we are not in a room): the 1:1 handler takes it. */
    data object NotOurs : MeshAction
    /** Ours, but nothing to do (stale or stranger frames that must not disturb the room). */
    data object Swallow : MeshAction
    /** A 1:1 call offer while we are in the Campfire: reply busy. */
    data class DeclineCall(val peer: String) : MeshAction
    /** A room offer from someone outside our roster (stale or racing): close that leg. */
    data class RefuseLeg(val peer: String) : MeshAction
    data class Answer(val peer: String, val sdp: String) : MeshAction
    data class RemoteAnswer(val peer: String, val sdp: String) : MeshAction
    data class Ice(val peer: String, val candidate: String, val sdpMid: String?, val sdpMLineIndex: Int) : MeshAction
    data class CloseLeg(val peer: String) : MeshAction
}

object CampfireSignaling {
    const val DEFAULT_ROOM = "*90001"
    /** Sent on join_room so the hub relays room_* (not legacy call_* + room_code) to us. */
    const val PROTOCOL = 2
    val ROOM_TYPES = setOf("room_offer", "room_answer", "room_ice", "room_leave")
    private val CALL_TYPES = setOf("call_offer", "call_answer", "ice_candidate", "call_hangup", "call_rejected")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Mirrors backend `dialplan.is_campfire_code`: `*9` followed by four digits. */
    fun isRoomCode(code: String) = code.length == 6 && code.startsWith("*9") && code.drop(1).all { it.isDigit() }

    /** Parses one text frame; null for anything that is not a room event (calls, ICE, junk). */
    fun parse(raw: String): RoomEvent? {
        val o = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull
        return when (s("type")) {
            "room_joined" -> {
                val room = s("room_code") ?: return null
                val peers = (o["peers"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
                RoomEvent.Joined(room, peers.distinct())
            }
            "peer_joined_room" -> RoomEvent.PeerJoined(s("room_code") ?: return null, s("peer_dial_code") ?: return null)
            "peer_left_room" -> RoomEvent.PeerLeft(s("room_code") ?: return null, s("peer_dial_code") ?: return null)
            "room_closed" -> RoomEvent.Closed(s("room_code") ?: return null, s("reason"))
            // The backend reports a refused join as call_rejected with from_dial_code = the room code.
            "call_rejected" -> s("from_dial_code")?.takeIf(::isRoomCode)?.let { RoomEvent.Rejected(it, s("reason") ?: "Couldn't join the Campfire.") }
            else -> null
        }
    }

    fun join(room: String, householdId: Int?): String = buildJsonObject {
        put("type", "join_room"); put("room_code", room); put("protocol", PROTOCOL)
        if (householdId != null) put("household_id", householdId)
    }.toString()

    /** Leaves the whole room (no target). */
    fun leave(room: String): String = buildJsonObject { put("type", "room_leave"); put("room_code", room) }.toString()

    /** Closes one mesh leg. */
    fun leaveLeg(peer: String, room: String?, reason: String? = null): String = buildJsonObject {
        put("type", "room_leave"); put("target_dial_code", peer)
        if (room != null) put("room_code", room)
        if (reason != null) put("reason", reason)
    }.toString()

    fun offer(peer: String, room: String?, sdp: String): String = sdpFrame("room_offer", peer, room, sdp)
    fun answer(peer: String, room: String?, sdp: String): String = sdpFrame("room_answer", peer, room, sdp)

    fun ice(peer: String, room: String?, candidate: String, sdpMid: String?, sdpMLineIndex: Int): String = buildJsonObject {
        put("type", "room_ice"); put("target_dial_code", peer)
        if (room != null) put("room_code", room)
        put("candidate", buildJsonObject {
            put("candidate", candidate)
            if (sdpMid != null) put("sdpMid", sdpMid)
            put("sdpMLineIndex", sdpMLineIndex)
        })
    }.toString()

    private fun sdpFrame(type: String, peer: String, room: String?, sdp: String) = buildJsonObject {
        put("type", type); put("target_dial_code", peer)
        if (room != null) put("room_code", room)
        put("sdp", sdp)
    }.toString()

    /**
     * Decides what to do with a mesh frame. Pure. A room leg is only accepted from a peer in our
     * roster, for our room, while we are in it (Android rule: "only answer peers in the room list").
     */
    fun meshAction(raw: String, state: CampfireState): MeshAction {
        val o = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return MeshAction.NotOurs
        fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull
        val type = s("type") ?: return MeshAction.NotOurs
        val from = s("from_dial_code") ?: return if (type in ROOM_TYPES) MeshAction.Swallow else MeshAction.NotOurs
        if (type in CALL_TYPES) {
            if (!state.inRoom) return MeshAction.NotOurs
            return if (type == "call_offer") MeshAction.DeclineCall(from) else MeshAction.Swallow
        }
        if (type !in ROOM_TYPES) return MeshAction.NotOurs
        val member = state.inRoom && s("room_code") == state.roomCode && from in state.peers
        if (!member) return if (type == "room_offer") MeshAction.RefuseLeg(from) else MeshAction.Swallow
        return when (type) {
            "room_offer" -> s("sdp")?.takeIf { it.isNotEmpty() }?.let { MeshAction.Answer(from, it) } ?: MeshAction.Swallow
            "room_answer" -> s("sdp")?.takeIf { it.isNotEmpty() }?.let { MeshAction.RemoteAnswer(from, it) } ?: MeshAction.Swallow
            "room_ice" -> {
                val c = o["candidate"] as? JsonObject ?: return MeshAction.Swallow
                fun cs(k: String) = (c[k] as? JsonPrimitive)?.contentOrNull
                val cand = cs("candidate")?.takeIf { it.isNotEmpty() } ?: return MeshAction.Swallow
                MeshAction.Ice(from, cand, cs("sdpMid")?.takeIf { it.isNotEmpty() }, cs("sdpMLineIndex")?.toIntOrNull() ?: 0)
            }
            else -> MeshAction.CloseLeg(from)
        }
    }
}

/** Immutable room state; [reduce] is the only way it changes, so it is trivially testable. */
data class CampfireState(
    val roomCode: String? = null,
    val joining: Boolean = false,
    /** Dial codes of the other participants (never includes us). */
    val peers: List<String> = emptyList(),
    /** Set once when a room ends or a join fails; the UI shows it then clears it. */
    val notice: String? = null,
) {
    val inRoom get() = roomCode != null

    fun reduce(e: RoomEvent, me: String = ""): CampfireState = when (e) {
        is RoomEvent.Joined -> copy(roomCode = e.roomCode, joining = false, peers = e.peers.filter { it != me }, notice = null)
        is RoomEvent.PeerJoined ->
            if (e.roomCode != roomCode || e.peer == me || e.peer in peers) this else copy(peers = peers + e.peer)
        is RoomEvent.PeerLeft -> if (e.roomCode != roomCode) this else copy(peers = peers - e.peer)
        is RoomEvent.Closed -> if (e.roomCode != roomCode) this else CampfireState(notice = e.reason ?: "The Campfire closed.")
        is RoomEvent.Rejected -> CampfireState(notice = e.reason)
    }
}
