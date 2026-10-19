package com.treefort.guardian

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Now" and the display zone. Captures (debug screen fixture) pin both; the app uses the device clock and zone. */
object DisplayClock {
    var now: () -> Instant = { Instant.now() }
    var zone: () -> ZoneId = { ZoneId.systemDefault() }
}

/** docs/fixtures/FORMATS.md, display rules. Pure JVM, unit tested. */
object Fmt {
    private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    fun time(t: Instant, zone: ZoneId) = TIME.format(t.atZone(zone))
    fun date(t: Instant, zone: ZoneId) = DATE.format(t.atZone(zone))
    fun dateTime(t: Instant, zone: ZoneId) = "${date(t, zone)} at ${time(t, zone)}"

    /** `Today`, `Yesterday`, else `EEE, MMM d`, comparing days in [zone]. */
    fun dayHeader(t: Instant, now: Instant, zone: ZoneId): String {
        val day = t.atZone(zone).toLocalDate(); val today = now.atZone(zone).toLocalDate()
        return when (day) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> DAY.format(t.atZone(zone))
        }
    }

    fun relative(t: Instant, now: Instant): String {
        val min = Duration.between(t, now).toMinutes()
        return when {
            min < 1 -> "Just now"
            min < 60 -> "$min min ago"
            min < 24 * 60 -> "${min / 60} hr ago"
            min < 48 * 60 -> "Yesterday"
            else -> "${min / (24 * 60)} days ago"
        }
    }

    /** `m:ss`, or `h:mm:ss` from one hour. */
    fun duration(seconds: Int): String {
        val h = seconds / 3600; val m = seconds % 3600 / 60; val s = seconds % 60
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}" else "$m:${s.toString().padStart(2, '0')}"
    }

    /** Duration slot of a call row: `Blocked`, `No answer` (never 0:00) or the duration. */
    fun callDuration(direction: String, seconds: Int) = when {
        direction.equals("intercepted", true) -> "Blocked"
        seconds <= 0 -> "No answer"
        else -> duration(seconds)
    }

    /** `Incoming on <phone>`, `Outgoing on <phone>`, `Blocked on <phone>`. */
    fun directionLine(direction: String, phoneName: String) = when (direction.lowercase()) {
        "inbound", "incoming" -> "Incoming on $phoneName"
        "intercepted" -> "Blocked on $phoneName"
        else -> "Outgoing on $phoneName"
    }
}
