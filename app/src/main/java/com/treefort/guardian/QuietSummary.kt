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
    fun line(rule: QuietRule) = "${time(rule.startTime)} to ${time(rule.endTime)}, ${rule.days.joinToString(", ")}"
}
