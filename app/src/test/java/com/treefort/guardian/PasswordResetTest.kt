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
}
