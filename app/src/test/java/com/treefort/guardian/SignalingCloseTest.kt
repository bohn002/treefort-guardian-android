package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalingCloseTest {
    @Test fun replacedSocketIsTerminalAndNotRetried() {
        assertEquals(4409, SignalingClose.REPLACED)
        assertTrue(SignalingClose.isTerminal(4409))
        assertFalse(SignalingClose.shouldAutoReconnect(4409))
    }

    @Test fun otherAbnormalClosesKeepReconnecting() {
        for (code in listOf(1001, 1006, 1011, 1012, 4401, 4408, 4410)) {
            assertFalse(SignalingClose.isTerminal(code))
            assertTrue("code $code should retry", SignalingClose.shouldAutoReconnect(code))
        }
    }

    @Test fun deliberateCloseDoesNotReconnect() {
        assertFalse(SignalingClose.shouldAutoReconnect(1000))
    }
}
