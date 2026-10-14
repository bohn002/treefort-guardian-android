package com.treefort.guardian

/**
 * Email sign-up confirmation loop. Pure JVM (no android.* imports) so it is unit tested in app/src/test.
 *
 * The confirmation email's link must land on [REDIRECT_URL] (a public page that says "Email confirmed.
 * Open the TreeFort app and sign in"), not on whatever the Supabase Site URL is. That URL has to be in
 * Supabase's Redirect URLs. This is independent of the OAuth build flags.
 */
object EmailConfirmation {
    const val REDIRECT_URL = "${AppConfig.WEB_APP_URL}/auth/confirmed"
    const val RESEND_COOLDOWN_SECONDS = 60

    /** Sign-in failed only because the address is not confirmed yet. */
    class NotConfirmed(message: String) : Exception(message)

    fun isNotConfirmedError(message: String?): Boolean {
        val m = message.orEmpty()
        return m.contains("not confirmed", ignoreCase = true) || m.contains("email_not_confirmed", ignoreCase = true)
    }

    /**
     * The landing page's convenience link `com.treefort.guardian://login-callback` carries nothing.
     * A URL with any token, code or error is the OAuth (Apple) callback and must keep going down that path.
     */
    fun isConfirmationCallback(fragment: String?, query: String?): Boolean {
        val raw = "${fragment.orEmpty()}&${query.orEmpty()}"
        val keys = raw.split('&', '#', '?').map { it.substringBefore('=') }
        return keys.none { it == "access_token" || it == "refresh_token" || it == "code" || it == "error" || it == "error_description" }
    }

    /** Whole seconds left before Resend is allowed again (0 = allowed). [lastSentAtMs] 0 means never sent. */
    fun cooldownRemaining(lastSentAtMs: Long, nowMs: Long, cooldownSeconds: Int = RESEND_COOLDOWN_SECONDS): Int {
        if (lastSentAtMs <= 0L) return 0
        val leftMs = lastSentAtMs + cooldownSeconds * 1000L - nowMs
        if (leftMs <= 0L) return 0
        return minOf(cooldownSeconds.toLong(), (leftMs + 999L) / 1000L).toInt()
    }
}
