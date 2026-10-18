package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordResetTest {
    @Test fun `validate enforces length then match`() {
        assertEquals(PasswordReset.TOO_SHORT, PasswordReset.validate("short", "short"))
        assertEquals(PasswordReset.MISMATCH, PasswordReset.validate("longenough", "different1"))
        assertNull(PasswordReset.validate("longenough", "longenough"))
    }

    @Test fun `recovery link yields token hash`() {
        assertEquals("abc123", PasswordReset.tokenHash("https", "app.treefort.kids", "/auth/confirmed", "token_hash=abc123&type=recovery"))
    }

    @Test fun `other links are not recovery`() {
        assertNull(PasswordReset.tokenHash("https", "app.treefort.kids", "/auth/confirmed", "token_hash=abc&type=signup"))
        assertNull(PasswordReset.tokenHash("https", "app.treefort.kids", "/auth/confirmed", null))
        assertNull(PasswordReset.tokenHash("https", "evil.example", "/auth/confirmed", "token_hash=abc&type=recovery"))
        assertNull(PasswordReset.tokenHash("com.treefort.guardian", "login-callback", "", "token_hash=abc&type=recovery"))
    }
}
