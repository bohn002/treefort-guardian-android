package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailConfirmationTest {
    @Test fun `redirect is the public confirmed page`() {
        assertEquals("https://app.treefort.kids/auth/confirmed", EmailConfirmation.REDIRECT_URL)
    }

    @Test fun `not confirmed errors are recognised`() {
        assertTrue(EmailConfirmation.isNotConfirmedError("Email not confirmed"))
        assertTrue(EmailConfirmation.isNotConfirmedError("error: email_not_confirmed"))
        assertFalse(EmailConfirmation.isNotConfirmedError("Invalid login credentials"))
        assertFalse(EmailConfirmation.isNotConfirmedError(null))
    }

    @Test fun `bare callback is the confirmation case`() {
        assertTrue(EmailConfirmation.isConfirmationCallback(null, null))
        assertTrue(EmailConfirmation.isConfirmationCallback("", ""))
    }

    @Test fun `token code or error callbacks are not the confirmation case`() {
        assertFalse(EmailConfirmation.isConfirmationCallback("access_token=a&refresh_token=b&expires_in=3600", null))
        assertFalse(EmailConfirmation.isConfirmationCallback(null, "code=abc"))
        assertFalse(EmailConfirmation.isConfirmationCallback("error=access_denied&error_description=no", null))
        assertFalse(EmailConfirmation.isConfirmationCallback(null, "error_description=x"))
    }

    @Test fun `cooldown counts down from 60 and never sent means allowed`() {
        assertEquals(0, EmailConfirmation.cooldownRemaining(0L, 5_000L))
        assertEquals(60, EmailConfirmation.cooldownRemaining(1_000L, 1_000L))
        assertEquals(60, EmailConfirmation.cooldownRemaining(1_000L, 1_001L))
        assertEquals(30, EmailConfirmation.cooldownRemaining(1_000L, 31_000L))
        assertEquals(1, EmailConfirmation.cooldownRemaining(1_000L, 60_500L))
        assertEquals(0, EmailConfirmation.cooldownRemaining(1_000L, 61_000L))
        assertEquals(60, EmailConfirmation.cooldownRemaining(10_000L, 5_000L))
    }
}
