package com.treefort.guardian

/** Forgot-password pure logic (no android.* imports, unit tested). */
object PasswordReset {
    const val MIN_LENGTH = 8
    const val NEUTRAL_SENT = "Check your email. If an account exists for that address, a reset link is on its way."
    const val TOO_SHORT = "Password must be at least 8 characters."
    const val MISMATCH = "Passwords don't match."

    /** Null = ok, else the inline error. */
    fun validate(password: String, confirm: String): String? = when {
        password.length < MIN_LENGTH -> TOO_SHORT
        password != confirm -> MISMATCH
        else -> null
    }

    /** The recovery email link `https://app.treefort.kids/auth/confirmed?token_hash=...&type=recovery`; null for anything else. */
    fun tokenHash(scheme: String?, host: String?, path: String?, query: String?): String? {
        if (scheme != "https" || host != "app.treefort.kids" || path != "/auth/confirmed") return null
        val p = query.orEmpty().split('&').associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('=', ""), "UTF-8") }
        return p["token_hash"]?.takeIf { it.isNotBlank() && p["type"] == "recovery" }
    }
}
