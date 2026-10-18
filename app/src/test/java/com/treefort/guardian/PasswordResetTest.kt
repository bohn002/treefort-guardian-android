package com.treefort.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordResetTest {
    private fun link(q: String?, host: String = "app.treefort.kids") = PasswordReset.parseLink("https", host, "/auth/confirmed", q)

    @Test fun `validate enforces length then match`() {
        assertEquals(PasswordReset.TOO_SHORT, PasswordReset.validate("short", "short"))
        assertEquals(PasswordReset.MISMATCH, PasswordReset.validate("longenough", "different1"))
        assertEquals(null, PasswordReset.validate("longenough", "longenough"))
    }

    @Test fun `recovery link yields token hash`() {
        assertEquals(PasswordReset.Link.Recovery("abc123"), link("token_hash=abc123&type=recovery"))
    }

    @Test fun `signup link is recognised`() {
        assertEquals(PasswordReset.Link.Signup, link("token_hash=abc&type=signup"))
    }

    @Test fun `other links are ignored`() {
        assertEquals(PasswordReset.Link.None, link("token_hash=abc&type=magiclink"))
        assertEquals(PasswordReset.Link.None, link("token_hash=&type=recovery"))
        assertEquals(PasswordReset.Link.None, link(null))
        assertEquals(PasswordReset.Link.None, link("token_hash=abc&type=recovery", host = "evil.example"))
        assertEquals(PasswordReset.Link.None, PasswordReset.parseLink("com.treefort.guardian", "login-callback", "", "token_hash=abc&type=recovery"))
    }

    @Test fun `malformed percent escape does not throw`() {
        assertEquals(PasswordReset.Link.None, link("token_hash=%zz&type=recovery"))
    }

    @Test fun `weak password server error is recognised`() {
        assertEquals(true, PasswordReset.isWeakPasswordError("Password should contain at least one character of each: abc, ABC, 123, !@#"))
        assertEquals(true, PasswordReset.isWeakPasswordError("weak_password"))
        assertEquals(false, PasswordReset.isWeakPasswordError("Invalid login credentials"))
        assertEquals(false, PasswordReset.isWeakPasswordError(null))
    }

    @Test fun `copy matches spec`() {
        assertEquals("Use at least 8 characters with upper and lower case letters, a number and a symbol.", PasswordReset.WEAK)
        assertEquals("Already have an account? Sign in or reset your password.", PasswordReset.ALREADY_HAVE_ACCOUNT)
    }

    @Test fun `otp disabled error is recognised`() {
        assertEquals(true, PasswordReset.isOtpDisabledError("Signups not allowed for otp"))
        assertEquals(true, PasswordReset.isOtpDisabledError("error_code: otp_disabled"))
        assertEquals(false, PasswordReset.isOtpDisabledError("rate limit"))
        assertEquals(false, PasswordReset.isOtpDisabledError(null))
    }
}
