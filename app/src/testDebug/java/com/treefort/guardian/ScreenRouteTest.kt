package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenRouteTest {
    @Test fun knownIdParsesWithTrim() {
        val r = ScreenRoute.parse(" settings-travel ")
        assertEquals("Trips", r?.sub)
        assertEquals(3, r?.tab)
    }

    @Test fun unknownOrMissingIdIsNull() {
        assertNull(ScreenRoute.parse(null))
        assertNull(ScreenRoute.parse("../../etc"))
        assertNull(ScreenRoute.parse("station-device-menu"))
    }

    @Test fun signedOutIdsNeverReachTheApp() {
        assertEquals(Phase.SignedOut, ScreenRoute.parse("auth-sign-in")?.phase)
        assertNotNull(ScreenRoute.parse("auth-certify-18"))
    }
}
