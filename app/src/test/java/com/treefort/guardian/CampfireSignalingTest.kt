package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampfireSignalingTest {
    private val room = "*90001"

    @Test fun roomCodesMatchTheBackendDialplan() {
        assertTrue(CampfireSignaling.isRoomCode("*90001"))
        assertFalse(CampfireSignaling.isRoomCode("*12345"))
        assertFalse(CampfireSignaling.isRoomCode("*9001"))
        assertFalse(CampfireSignaling.isRoomCode("*9abcd"))
    }

    @Test fun parsesRoomJoinedWithPeers() {
        val e = CampfireSignaling.parse("""{"type":"room_joined","room_code":"*90001","peers":["*11111","*22222","*11111"]}""")
        assertEquals(RoomEvent.Joined(room, listOf("*11111", "*22222")), e)
    }

    @Test fun parsesPeerJoinedLeftAndClosed() {
        assertEquals(RoomEvent.PeerJoined(room, "*3"), CampfireSignaling.parse("""{"type":"peer_joined_room","room_code":"*90001","peer_dial_code":"*3"}"""))
        assertEquals(RoomEvent.PeerLeft(room, "*3"), CampfireSignaling.parse("""{"type":"peer_left_room","room_code":"*90001","peer_dial_code":"*3"}"""))
        assertEquals(
            RoomEvent.Closed(room, "You are no longer a member of this household"),
            CampfireSignaling.parse("""{"type":"room_closed","room_code":"*90001","reason":"You are no longer a member of this household"}"""),
        )
    }

    @Test fun refusedJoinIsACallRejectedNamingTheRoom() {
        val e = CampfireSignaling.parse("""{"type":"call_rejected","from_dial_code":"*90001","reason":"Campfire is only for your own household"}""")
        assertEquals(RoomEvent.Rejected(room, "Campfire is only for your own household"), e)
        // A normal 1:1 rejection is not a room event.
        assertNull(CampfireSignaling.parse("""{"type":"call_rejected","from_dial_code":"*12345","reason":"x"}"""))
    }

    @Test fun ignoresCallsJunkAndIncompleteFrames() {
        assertNull(CampfireSignaling.parse("""{"type":"call_offer","from_dial_code":"*1","sdp":"v=0"}"""))
        assertNull(CampfireSignaling.parse("not json"))
        assertNull(CampfireSignaling.parse("""{"type":"peer_left_room","room_code":"*90001"}"""))
        assertNull(CampfireSignaling.parse("""[1,2]"""))
    }

    @Test fun joinAndLeaveFramesCarryHouseholdOnlyWhenGiven() {
        assertEquals("""{"type":"join_room","room_code":"*90001","protocol":2,"household_id":7}""", CampfireSignaling.join(room, 7))
        assertEquals("""{"type":"join_room","room_code":"*90001","protocol":2}""", CampfireSignaling.join(room, null))
        assertEquals("""{"type":"room_leave","room_code":"*90001"}""", CampfireSignaling.leave(room))
    }

    @Test fun legFramesUseRoomTypesNeverCallTypes() {
        assertEquals("""{"type":"room_offer","target_dial_code":"*5","room_code":"*90001","sdp":"v=0"}""", CampfireSignaling.offer("*5", room, "v=0"))
        assertEquals("""{"type":"room_answer","target_dial_code":"*5","room_code":"*90001","sdp":"v=1"}""", CampfireSignaling.answer("*5", room, "v=1"))
        assertEquals(
            """{"type":"room_ice","target_dial_code":"*5","room_code":"*90001","candidate":{"candidate":"candidate:1","sdpMid":"0","sdpMLineIndex":0}}""",
            CampfireSignaling.ice("*5", room, "candidate:1", "0", 0),
        )
        assertEquals("""{"type":"room_leave","target_dial_code":"*5","room_code":"*90001"}""", CampfireSignaling.leaveLeg("*5", room))
        assertEquals("""{"type":"room_leave","target_dial_code":"*5","reason":"full"}""", CampfireSignaling.leaveLeg("*5", null, "full"))
    }

    private val inRoom = CampfireState(roomCode = room, peers = listOf("*1", "*2"))

    @Test fun roomLegsFromRosterPeersAreAccepted() {
        assertEquals(MeshAction.Answer("*1", "v=0"),
            CampfireSignaling.meshAction("""{"type":"room_offer","from_dial_code":"*1","room_code":"*90001","sdp":"v=0"}""", inRoom))
        assertEquals(MeshAction.RemoteAnswer("*2", "v=1"),
            CampfireSignaling.meshAction("""{"type":"room_answer","from_dial_code":"*2","room_code":"*90001","sdp":"v=1"}""", inRoom))
        assertEquals(MeshAction.Ice("*1", "candidate:1", "0", 0),
            CampfireSignaling.meshAction("""{"type":"room_ice","from_dial_code":"*1","room_code":"*90001","candidate":{"candidate":"candidate:1","sdpMid":"0","sdpMLineIndex":0}}""", inRoom))
        assertEquals(MeshAction.CloseLeg("*2"),
            CampfireSignaling.meshAction("""{"type":"room_leave","from_dial_code":"*2","room_code":"*90001"}""", inRoom))
    }

    @Test fun roomLegsFromOutsideTheRosterOrRoomAreNeverAnswered() {
        // Stranger, wrong room, or not in a room at all: an offer gets its leg closed, the rest is dropped.
        assertEquals(MeshAction.RefuseLeg("*9"),
            CampfireSignaling.meshAction("""{"type":"room_offer","from_dial_code":"*9","room_code":"*90001","sdp":"v=0"}""", inRoom))
        assertEquals(MeshAction.RefuseLeg("*1"),
            CampfireSignaling.meshAction("""{"type":"room_offer","from_dial_code":"*1","room_code":"*90002","sdp":"v=0"}""", inRoom))
        assertEquals(MeshAction.RefuseLeg("*1"),
            CampfireSignaling.meshAction("""{"type":"room_offer","from_dial_code":"*1","room_code":"*90001","sdp":"v=0"}""", CampfireState()))
        assertEquals(MeshAction.Swallow,
            CampfireSignaling.meshAction("""{"type":"room_leave","from_dial_code":"*9","room_code":"*90001"}""", inRoom))
        assertEquals(MeshAction.Swallow,
            CampfireSignaling.meshAction("""{"type":"room_ice","from_dial_code":"*1","room_code":"*90001","candidate":{"candidate":"c"}}""", CampfireState(joining = true)))
    }

    @Test fun plainCallsAreNeverRoomLegs() {
        // In a room: a 1:1 offer (even from a room member) is declined busy; other call frames are dropped.
        assertEquals(MeshAction.DeclineCall("*1"),
            CampfireSignaling.meshAction("""{"type":"call_offer","from_dial_code":"*1","room_code":"*90001","sdp":"v=0"}""", inRoom))
        assertEquals(MeshAction.Swallow, CampfireSignaling.meshAction("""{"type":"call_hangup","from_dial_code":"*1"}""", inRoom))
        // Not in a room: 1:1 frames belong to the call handler.
        assertEquals(MeshAction.NotOurs, CampfireSignaling.meshAction("""{"type":"call_offer","from_dial_code":"*1","sdp":"v=0"}""", CampfireState()))
        assertEquals(MeshAction.NotOurs, CampfireSignaling.meshAction("""{"type":"pong"}""", inRoom))
        assertEquals(MeshAction.NotOurs, CampfireSignaling.meshAction("junk", inRoom))
    }

    @Test fun rosterFollowsJoinsAndLeaves() {
        var s = CampfireState(joining = true)
        s = s.reduce(RoomEvent.Joined(room, listOf("*1", "*me", "*2")), me = "*me")
        assertTrue(s.inRoom); assertFalse(s.joining)
        assertEquals(listOf("*1", "*2"), s.peers)
        s = s.reduce(RoomEvent.PeerJoined(room, "*3"), "*me")
        s = s.reduce(RoomEvent.PeerJoined(room, "*3"), "*me") // duplicate
        s = s.reduce(RoomEvent.PeerJoined(room, "*me"), "*me") // self
        assertEquals(listOf("*1", "*2", "*3"), s.peers)
        s = s.reduce(RoomEvent.PeerLeft(room, "*1"), "*me")
        assertEquals(listOf("*2", "*3"), s.peers)
    }

    @Test fun eventsForOtherRoomsAreIgnored() {
        val s = CampfireState(roomCode = room, peers = listOf("*1"))
        assertEquals(s, s.reduce(RoomEvent.PeerLeft("*90002", "*1")))
        assertEquals(s, s.reduce(RoomEvent.PeerJoined("*90002", "*9")))
        assertEquals(s, s.reduce(RoomEvent.Closed("*90002", "x")))
    }

    @Test fun roomClosedEmptiesTheRosterAndExplains() {
        val s = CampfireState(roomCode = room, peers = listOf("*1")).reduce(RoomEvent.Closed(room, "Removed"))
        assertFalse(s.inRoom); assertTrue(s.peers.isEmpty()); assertEquals("Removed", s.notice)
        assertEquals("The Campfire closed.", CampfireState(roomCode = room).reduce(RoomEvent.Closed(room, null)).notice)
    }

    @Test fun rejectedJoinLeavesTheRoomWithTheReason() {
        val s = CampfireState(joining = true).reduce(RoomEvent.Rejected(room, "Campfire is only for your own household"))
        assertFalse(s.joining); assertFalse(s.inRoom)
        assertEquals("Campfire is only for your own household", s.notice)
    }
}
