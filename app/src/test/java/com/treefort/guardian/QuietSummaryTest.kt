package com.treefort.guardian

import com.treefort.guardian.api.QuietRule
import org.junit.Assert.assertEquals
import org.junit.Test

class QuietSummaryTest {
    private val rule = QuietRule("r1", "School nights", true, listOf("Sun", "Mon", "Tue", "Wed", "Thu"), "20:00", "07:00")

    @Test fun `active label matches iOS`() {
        assertEquals("No schedules yet", QuietSummary.activeLabel(0))
        assertEquals("1 active schedule", QuietSummary.activeLabel(1))
        assertEquals("2 active schedules", QuietSummary.activeLabel(2))
    }

    @Test fun `times are 12 hour`() {
        assertEquals("8:00 PM", QuietSummary.time("20:00"))
        assertEquals("7:00 AM", QuietSummary.time("07:00"))
        assertEquals("12:05 AM", QuietSummary.time("00:05"))
        assertEquals("12:30 PM", QuietSummary.time("12:30"))
        assertEquals("soon", QuietSummary.time("soon"))
    }

    @Test fun `rule line and title`() {
        assertEquals("Sun-Thu, 8:00 PM - 7:00 AM", QuietSummary.line(rule))
        assertEquals("Mon, Wed", QuietSummary.days(listOf("Mon", "Wed")))
        assertEquals("Sat, Sun, Mon", QuietSummary.days(listOf("Sat", "Sun", "Mon")))
        assertEquals("School nights", QuietSummary.title(rule))
        assertEquals("Quiet Hours", QuietSummary.title(rule.copy(name = null)))
    }
}
