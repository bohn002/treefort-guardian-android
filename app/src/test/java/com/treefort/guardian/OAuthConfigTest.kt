package com.treefort.guardian

import com.treefort.guardian.OAuthConfig.Availability
import com.treefort.guardian.OAuthConfig.CallbackResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OAuthConfigTest {
    private val clientId = "1234567890-abcdefghijklmnop.apps.googleusercontent.com"

    private fun settings(google: String? = null, apple: String? = null, id: String? = null, supabase: Boolean = true) =
        OAuthConfig.Settings(google, apple, id, supabase)

    @Test fun `flags unset - both providers off and nothing shown`() {
        val s = settings()
        assertEquals(Availability.Off, s.google)
        assertEquals(Availability.Off, s.apple)
        assertFalse(s.anyVisible)
    }

    @Test fun `this build ships with both flags off`() {
        // CI and fresh checkouts set no TREEFORT_OAUTH_* values, so the compiled build must be off.
        if (OAuthConfig.isOn(BuildConfig.OAUTH_GOOGLE_ENABLED) || OAuthConfig.isOn(BuildConfig.OAUTH_APPLE_ENABLED)) return
        assertFalse(OAuthConfig.current.anyVisible)
    }

    @Test fun `only literal true turns a flag on`() {
        for (v in listOf("", "1", "yes", "on", "false", "truthy", " ")) {
            assertFalse("'$v' must be off", OAuthConfig.isOn(v))
            assertEquals(Availability.Off, settings(google = v, apple = v, id = clientId).google)
        }
        assertTrue(OAuthConfig.isOn(" TRUE "))
    }

    @Test fun `google on without a web client id is shown disabled with a reason`() {
        val g = settings(google = "true").google
        assertTrue(g is Availability.Misconfigured)
        assertTrue((g as Availability.Misconfigured).reason.contains("GOOGLE_WEB_CLIENT_ID"))
        assertTrue(settings(google = "true").anyVisible)
    }

    @Test fun `google rejects an id that is not a web client id`() {
        assertTrue(settings(google = "true", id = "not-a-client-id").google is Availability.Misconfigured)
        assertTrue(settings(google = "true", id = "your_value_here").google is Availability.Misconfigured)
    }

    @Test fun `google ready with flag and client id`() {
        assertEquals(Availability.Ready, settings(google = "true", id = clientId).google)
        assertEquals(Availability.Off, settings(google = "true", id = clientId).apple)
    }

    @Test fun `apple needs only its flag in the app`() {
        assertEquals(Availability.Ready, settings(apple = "true").apple)
        assertEquals(Availability.Off, settings(apple = "true").google)
    }

    @Test fun `missing supabase config disables both even when flagged`() {
        val s = settings(google = "true", apple = "true", id = clientId, supabase = false)
        assertTrue(s.google is Availability.Misconfigured)
        assertTrue(s.apple is Availability.Misconfigured)
    }

    @Test fun `not set up message is plain and names the provider`() {
        assertEquals("Sign in with Apple is not set up yet. Use email for now.", OAuthConfig.notSetUpMessage("Apple"))
        assertEquals("Sign in with Google is not set up yet. Use email for now.", OAuthConfig.notSetUpMessage("Google"))
    }

    @Test fun `callback deep link matching`() {
        assertTrue(OAuthConfig.isCallback("com.treefort.guardian", "login-callback"))
        assertFalse(OAuthConfig.isCallback("https", "login-callback"))
        assertFalse(OAuthConfig.isCallback("com.treefort.guardian", "evil"))
        assertFalse(OAuthConfig.isCallback(null, null))
        assertEquals("com.treefort.guardian://login-callback", OAuthConfig.CALLBACK_URL)
    }

    @Test fun `manifest deep link matches the callback constant`() {
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml").map(::File).first { it.exists() }.readText()
        assertTrue(manifest.contains("android:scheme=\"${OAuthConfig.CALLBACK_SCHEME}\""))
        assertTrue(manifest.contains("android:host=\"${OAuthConfig.CALLBACK_HOST}\""))
    }

    @Test fun `callback fragment with tokens`() {
        val r = OAuthConfig.parseCallback("access_token=a.b.c&refresh_token=r1&expires_in=3600&token_type=bearer", null)
        assertEquals(CallbackResult.Tokens("a.b.c", "r1", 3600L, "bearer"), r)
    }

    @Test fun `callback error is surfaced and decoded`() {
        val r = OAuthConfig.parseCallback("error=access_denied&error_description=User%20cancelled", null)
        assertEquals(CallbackResult.Error("User cancelled"), r)
        assertTrue(OAuthConfig.parseCallback(null, "error=server_error") is CallbackResult.Error)
    }

    @Test fun `callback without tokens is empty`() {
        assertEquals(CallbackResult.Empty, OAuthConfig.parseCallback(null, null))
        assertEquals(CallbackResult.Empty, OAuthConfig.parseCallback("access_token=only", null))
    }

    @Test fun `nonce is random and hashed for google`() {
        val a = OAuthConfig.newRawNonce()
        assertNotEquals(a, OAuthConfig.newRawNonce())
        assertEquals(64, a.length)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", OAuthConfig.sha256Hex("abc"))
    }
}
