package com.treefort.guardian

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.OtpType
import kotlinx.coroutines.flow.StateFlow

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
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun sendEmailCode(email: String) {
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = false
        }
    }

    suspend fun verifyEmailCode(email: String, code: String) {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
