package com.treefort.guardian

import kotlinx.serialization.json.JsonArray
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

object CampfireSignaling {
    const val DEFAULT_ROOM = "*90001"
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
        put("type", "join_room"); put("room_code", room)
        if (householdId != null) put("household_id", householdId)
    }.toString()

    fun leave(room: String): String = buildJsonObject { put("type", "leave_room"); put("room_code", room) }.toString()

    /** Reads `{"type":..,"from_dial_code":..}` for mesh call frames; null if not a call frame. */
    fun meshFrom(raw: String): Pair<String, String>? {
        val o = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val type = o["type"]?.jsonPrimitive?.contentOrNull ?: return null
        val from = o["from_dial_code"]?.jsonPrimitive?.contentOrNull ?: return null
        return type to from
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
