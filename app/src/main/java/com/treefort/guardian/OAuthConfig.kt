package com.treefort.guardian

import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * "Sign in with Google / Apple" through Supabase Auth (ADR 0001). See docs/OAUTH_SETUP.md.
 *
 * Both providers are OFF unless the build sets TREEFORT_OAUTH_GOOGLE_ENABLED /
 * TREEFORT_OAUTH_APPLE_ENABLED to exactly "true" (gradle property, env or local.properties; never
 * committed). Google also needs GOOGLE_WEB_CLIENT_ID. No secrets live in the app: the Google web
 * client secret and the Apple key stay in the Supabase dashboard.
 *
 * Pure JVM (no android.* imports) so it is unit tested in app/src/test.
 */
object OAuthConfig {
    /** Deep link the browser returns to after Apple sign-in. Must be in Supabase's Redirect URLs. */
    const val CALLBACK_SCHEME = "com.treefort.guardian"
    const val CALLBACK_HOST = "login-callback"
    const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST"

    sealed interface Availability {
        /** Flag off: the button is not shown at all. */
        data object Off : Availability
        /** Flag on and everything the app needs is present. */
        data object Ready : Availability
        /** Flag on but the build is missing something: shown disabled with this explanation. */
        data class Misconfigured(val reason: String) : Availability
    }

    data class Settings(
        val googleFlag: String?,
        val appleFlag: String?,
        val googleWebClientId: String?,
        val supabaseConfigured: Boolean,
    ) {
        val google: Availability get() = when {
            !isOn(googleFlag) -> Availability.Off
            !supabaseConfigured -> Availability.Misconfigured("This build has no TreeFort server settings.")
            !looksLikeGoogleClientId(googleWebClientId) ->
                Availability.Misconfigured("Google sign-in is on, but this build has no GOOGLE_WEB_CLIENT_ID.")
            else -> Availability.Ready
        }

        val apple: Availability get() = when {
            !isOn(appleFlag) -> Availability.Off
            !supabaseConfigured -> Availability.Misconfigured("This build has no TreeFort server settings.")
            else -> Availability.Ready
        }

        val anyVisible: Boolean get() = google != Availability.Off || apple != Availability.Off
    }

    /** Shown when a provider button is tapped but the provider is not usable in this build (never a raw error). */
    fun notSetUpMessage(provider: String): String = "Sign in with $provider is not set up yet. Use email for now."

    /** Fail-closed: only the literal "true" (any case, trimmed) turns a provider on. */
    fun isOn(flag: String?): Boolean = flag?.trim()?.equals("true", ignoreCase = true) == true

    /** A Google OAuth *web* client ID (not the Android one): `<digits>-<id>.apps.googleusercontent.com`. */
    fun looksLikeGoogleClientId(id: String?): Boolean =
        id != null && Regex("^[0-9]+-[A-Za-z0-9_]+\\.apps\\.googleusercontent\\.com$").matches(id.trim())

    /** The build's settings (see app/build.gradle.kts). */
    val current: Settings
        get() = Settings(
            googleFlag = BuildConfig.OAUTH_GOOGLE_ENABLED,
            appleFlag = BuildConfig.OAUTH_APPLE_ENABLED,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            supabaseConfigured = !AppConfig.missingConfig,
        )

    fun isCallback(scheme: String?, host: String?): Boolean =
        scheme.equals(CALLBACK_SCHEME, ignoreCase = true) && host.equals(CALLBACK_HOST, ignoreCase = true)

    /** Tokens Supabase puts in the callback fragment (implicit flow) or an error it reports. */
    sealed interface CallbackResult {
        data class Tokens(val accessToken: String, val refreshToken: String, val expiresIn: Long, val tokenType: String) : CallbackResult
        data class Error(val message: String) : CallbackResult
        data object Empty : CallbackResult
    }

    fun parseCallback(fragment: String?, query: String?): CallbackResult {
        val f = params(fragment)
        val q = params(query)
        val err = f["error_description"] ?: q["error_description"] ?: f["error"] ?: q["error"]
        if (err != null) return CallbackResult.Error(err.ifBlank { "Sign-in was cancelled or failed." })
        val access = f["access_token"]
        val refresh = f["refresh_token"]
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) return CallbackResult.Empty
        return CallbackResult.Tokens(
            accessToken = access,
            refreshToken = refresh,
            expiresIn = f["expires_in"]?.toLongOrNull() ?: 3600L,
            tokenType = f["token_type"] ?: "bearer",
        )
    }

    private fun params(raw: String?): Map<String, String> =
        raw.orEmpty().removePrefix("#").removePrefix("?").split('&').mapNotNull { pair ->
            val i = pair.indexOf('=')
            if (i <= 0) null else pair.substring(0, i) to URLDecoder.decode(pair.substring(i + 1), "UTF-8")
        }.toMap()

    /** Random nonce for Google ID tokens: Google gets the SHA-256 hex, Supabase the raw value. */
    fun newRawNonce(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
