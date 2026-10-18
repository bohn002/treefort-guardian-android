package com.treefort.guardian

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.SessionSource
import io.github.jan.supabase.gotrue.providers.Apple
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Supabase Auth is the only identity provider (ADR 0001). This object signs in and hands out the
 * access token for the backend's Bearer header. It never reads or writes data tables: all data
 * goes through the backend API ([com.treefort.guardian.api.TreeFortApi]).
 */
object SupabaseManager {
    val client: SupabaseClient by lazy {
        createSupabaseClient(supabaseUrl = AppConfig.supabaseUrl, supabaseKey = AppConfig.supabaseAnonKey) {
            install(Auth)
        }
    }

    val sessionStatus: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    /** The current access token, or null when signed out. Refreshed by the SDK before it expires. */
    fun accessToken(): String? = if (AppConfig.missingConfig) null else client.auth.currentAccessTokenOrNull()

    suspend fun refresh() { client.auth.refreshCurrentSession() }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Returns true when a session exists right away (email confirmation off), false when the user must confirm first. */
    suspend fun signUp(email: String, password: String): Boolean {
        client.auth.signUpWith(Email, redirectUrl = EmailConfirmation.REDIRECT_URL) {
            this.email = email
            this.password = password
        }
        return client.auth.currentSessionOrNull() != null
    }

    /**
     * Resends the sign-up confirmation email with the same redirect as [signUp]. supabase-kt's
     * resendEmail has no redirect parameter, so this calls the GoTrue /resend endpoint directly.
     */
    suspend fun resendSignup(email: String) = withContext(Dispatchers.IO) {
        val url = (AppConfig.supabaseUrl.trimEnd('/') + "/auth/v1/resend").toHttpUrl().newBuilder()
            .addQueryParameter("redirect_to", EmailConfirmation.REDIRECT_URL).build()
        val body = buildJsonObject { put("type", "signup"); put("email", email) }.toString()
        val req = Request.Builder().url(url)
            .header("apikey", AppConfig.supabaseAnonKey)
            .post(body.toRequestBody("application/json".toMediaType())).build()
        resendHttp.newCall(req).execute().use { r ->
            if (!r.isSuccessful) {
                val text = r.body?.string().orEmpty()
                throw IllegalStateException(if (r.code == 429 || text.contains("rate", true)) "rate limit" else "resend failed (${r.code})")
            }
        }
    }

    private val resendHttp = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    suspend fun sendResetEmail(email: String) {
        client.auth.resetPasswordForEmail(email, redirectUrl = EmailConfirmation.REDIRECT_URL)
    }

    /** Exchanges the recovery link's token_hash for a session; returns the account email. */
    suspend fun verifyRecovery(tokenHash: String): String? {
        client.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, tokenHash = tokenHash)
        return client.auth.currentUserOrNull()?.email
    }

    suspend fun updatePassword(newPassword: String) { client.auth.updateUser { password = newPassword } }

    suspend fun sendEmailCode(email: String) {
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = false
        }
    }

    suspend fun verifyEmailCode(email: String, code: String) {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    /**
     * Native Google sign-in: the Google ID token (from Credential Manager) is exchanged for a Supabase
     * session. Supabase verifies the token's audience against the client IDs set on its Google provider
     * and the nonce against [rawNonce]. Lands in the same session flow as email sign-in.
     */
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String) {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    /**
     * Web-based Apple sign-in (Android has no native Apple flow): opens Supabase's authorize URL in a
     * Custom Tab. Apple returns to Supabase, which redirects to [OAuthConfig.CALLBACK_URL]; MainActivity
     * hands that intent to [importOAuthCallback]. The redirect is passed per call so email sign-up
     * keeps its own (Site URL) redirect.
     */
    suspend fun startAppleWebSignIn() {
        client.auth.signInWith(Apple, redirectUrl = OAuthConfig.CALLBACK_URL)
    }

    /** Stores the session from the OAuth callback deep link (implicit flow fragment). */
    suspend fun importOAuthCallback(fragment: String?, query: String?): Result<Unit> =
        when (val r = OAuthConfig.parseCallback(fragment, query)) {
            is OAuthConfig.CallbackResult.Error -> Result.failure(IllegalStateException(r.message))
            OAuthConfig.CallbackResult.Empty -> Result.failure(IllegalStateException("Sign-in did not complete. Please try again."))
            is OAuthConfig.CallbackResult.Tokens -> runCatching {
                val user = client.auth.retrieveUser(r.accessToken)
                client.auth.importSession(
                    UserSession(
                        accessToken = r.accessToken,
                        refreshToken = r.refreshToken,
                        expiresIn = r.expiresIn,
                        tokenType = r.tokenType,
                        user = user,
                    ),
                    source = SessionSource.External,
                )
            }
        }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
