package com.treefort.guardian

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Google and Apple sign-in, both ending in a Supabase session (ADR 0001). Each entry point re-checks
 * its build flag, so nothing reaches Google, Apple or Supabase while a provider is off. A new account
 * is still stopped by the 18+ certification screen (AppStore Phase.NeedsCertify), because the
 * backend's /api/auth/me reports is_guardian_certified=false until /api/auth/certify succeeds.
 */
object OAuthSignIn {
    class Cancelled : Exception("Sign-in cancelled.")

    /** Native Google sign-in through Credential Manager. [activityContext] must be an Activity. */
    suspend fun google(activityContext: Context): Result<Unit> {
        val cfg = OAuthConfig.current
        if (cfg.google != OAuthConfig.Availability.Ready) {
            return Result.failure(IllegalStateException("Sign in with Google isn't set up in this build."))
        }
        val rawNonce = OAuthConfig.newRawNonce()
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID.trim())
            .setNonce(OAuthConfig.sha256Hex(rawNonce))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(IllegalStateException("Google didn't return an ID token."))
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            SupabaseManager.signInWithGoogleIdToken(idToken, rawNonce)
            Result.success(Unit)
        } catch (_: GetCredentialCancellationException) {
            Result.failure(Cancelled())
        } catch (_: NoCredentialException) {
            Result.failure(IllegalStateException("No Google account is available on this device. Add one in Settings, or use email."))
        } catch (e: GetCredentialException) {
            Result.failure(IllegalStateException(e.message ?: "Google sign-in failed."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Web-based Apple sign-in (Custom Tab). The session arrives later via the callback deep link. */
    suspend fun apple(): Result<Unit> {
        if (OAuthConfig.current.apple != OAuthConfig.Availability.Ready) {
            return Result.failure(IllegalStateException("Sign in with Apple isn't set up in this build."))
        }
        return runCatching { SupabaseManager.startAppleWebSignIn() }
    }

    /** Called by MainActivity for `com.treefort.guardian://login-callback#...`. Ignored while Apple is off. */
    suspend fun handleCallback(scheme: String?, host: String?, fragment: String?, query: String?): Result<Unit>? {
        if (!OAuthConfig.isCallback(scheme, host)) return null
        if (OAuthConfig.current.apple != OAuthConfig.Availability.Ready) {
            return Result.failure(IllegalStateException("Sign in with Apple isn't set up in this build."))
        }
        return SupabaseManager.importOAuthCallback(fragment, query)
    }
}
