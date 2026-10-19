package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FmtTest {
    private val zone = ZoneId.of("America/Chicago")
    private val now = Instant.parse("2026-10-14T18:00:00Z")
    private fun i(s: String) = Instant.parse(s)

    @Test fun `fixture values from FORMATS`() {
        assertEquals("Oct 14, 2026 at 11:46 AM", Fmt.dateTime(i("2026-10-14T16:46:00Z"), zone))
        assertEquals("Oct 13, 2026 at 11:46 AM", Fmt.dateTime(i("2026-10-13T16:46:00Z"), zone))
        assertEquals("Oct 12, 2026 at 10:46 AM", Fmt.dateTime(i("2026-10-12T15:46:00Z"), zone))
        assertEquals("Oct 11, 2026 at 5:40 PM", Fmt.dateTime(i("2026-10-11T22:40:00Z"), zone))
        assertEquals("1:39 AM", Fmt.time(i("2026-10-14T06:39:00Z"), zone))
        assertEquals("5:12 AM", Fmt.time(i("2026-10-12T10:12:00Z"), zone))
    }

    @Test fun `day headers`() {
        assertEquals("Today", Fmt.dayHeader(i("2026-10-14T16:16:00Z"), now, zone))
        assertEquals("Yesterday", Fmt.dayHeader(i("2026-10-13T16:16:00Z"), now, zone))
        assertEquals("Mon, Oct 12", Fmt.dayHeader(i("2026-10-12T10:12:00Z"), now, zone))
    }

    @Test fun `relative last seen`() {
        assertEquals("14 min ago", Fmt.relative(i("2026-10-14T17:46:00Z"), now))
        assertEquals("19 hr ago", Fmt.relative(i("2026-10-13T22:19:00Z"), now))
        assertEquals("Just now", Fmt.relative(i("2026-10-14T17:59:30Z"), now))
        assertEquals("Yesterday", Fmt.relative(i("2026-10-13T00:00:00Z"), now))
        assertEquals("3 days ago", Fmt.relative(i("2026-10-11T12:00:00Z"), now))
    }

    @Test fun `durations`() {
        assertEquals("5:12", Fmt.duration(312))
        assertEquals("1:35", Fmt.duration(95))
        assertEquals("0:41", Fmt.duration(41))
        assertEquals("1:05:09", Fmt.duration(3909))
        assertEquals("No answer", Fmt.callDuration("inbound", 0))
        assertEquals("Blocked", Fmt.callDuration("intercepted", 0))
        assertEquals("5:12", Fmt.callDuration("inbound", 312))
    }

    @Test fun `direction lines`() {
        assertEquals("Incoming on Maya's TreeFort", Fmt.directionLine("inbound", "Maya's TreeFort"))
        assertEquals("Outgoing on Maya's TreeFort", Fmt.directionLine("outbound", "Maya's TreeFort"))
        assertEquals("Blocked on Leo's TreeFort", Fmt.directionLine("intercepted", "Leo's TreeFort"))
    }
}
