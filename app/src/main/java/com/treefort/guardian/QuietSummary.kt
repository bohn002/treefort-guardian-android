package com.treefort.guardian

import com.treefort.guardian.api.QuietRule

/** Copy for the Quiet Hours row on a phone card (same wording as iOS). Pure JVM, unit tested. */
object QuietSummary {
    fun activeLabel(active: Int) = when (active) {
        0 -> "No schedules yet"
        1 -> "1 active schedule"
        else -> "$active active schedules"
    }

    /** "20:00" to "8:00 PM"; anything unparseable comes back unchanged. */
    fun time(hhmm: String): String {
        val parts = hhmm.split(':')
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return hhmm
        return "${if (h % 12 == 0) 12 else h % 12}:${m.toString().padStart(2, '0')} ${if (h < 12) "AM" else "PM"}"
    }

    fun title(rule: QuietRule) = rule.name?.takeIf { it.isNotBlank() } ?: "Quiet Hours"
    private val WEEK = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    /** A run of 3+ consecutive days reads `Sun-Thu`; anything else lists the days. */
    fun days(days: List<String>): String {
        val idx = days.map { WEEK.indexOf(it) }
        val run = idx.size >= 3 && idx.all { it >= 0 } && idx.zipWithNext().all { (a, b) -> b == a + 1 }
        return if (run) "${days.first()}-${days.last()}" else days.joinToString(", ")
    }

    /** `Sun-Thu, 8:00 PM - 7:00 AM` (FORMATS.md). */
    fun line(rule: QuietRule) = "${days(rule.days)}, ${time(rule.startTime)} - ${time(rule.endTime)}"
}
