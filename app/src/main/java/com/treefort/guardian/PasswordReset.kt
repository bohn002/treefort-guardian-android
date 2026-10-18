package com.treefort.guardian

/** Forgot-password pure logic (no android.* imports, unit tested). */
object PasswordReset {
    const val MIN_LENGTH = 8
    const val NEUTRAL_SENT = "Check your email. If an account exists for that address, a reset link is on its way."
    const val TOO_SHORT = "Password must be at least 8 characters."
    const val MISMATCH = "Passwords don't match."

    const val ALREADY_HAVE_ACCOUNT = "Already have an account? Sign in or reset your password."
    const val CODE_SENT = "If an account exists for that address, a code is on its way."

    /** 422 otp_disabled "Signups not allowed for otp": unknown email on the code flow. Must look like success. */
    fun isOtpDisabledError(message: String?): Boolean {
        val m = message.orEmpty()
        return m.contains("otp_disabled", true) || m.contains("Signups not allowed", true)
    }

    const val WEAK = "Use at least 8 characters with upper and lower case letters, a number and a symbol."

    /** Supabase's password-strength rejection ("Password should contain at least one character of each..."). */
    fun isWeakPasswordError(message: String?): Boolean {
        val m = message.orEmpty()
        return m.contains("weak_password", true) || m.contains("Password should", true)
    }

    /** Null = ok, else the inline error. */
    fun validate(password: String, confirm: String): String? = when {
        password.length < MIN_LENGTH -> TOO_SHORT
        password != confirm -> MISMATCH
        else -> null
    }

    sealed interface Link {
        data class Recovery(val tokenHash: String) : Link
        data object Signup : Link
        data object None : Link
    }

    /** Classifies an `https://app.treefort.kids/auth/confirmed?...` link; only types recovery and signup are accepted. */
    fun parseLink(scheme: String?, host: String?, path: String?, query: String?): Link {
        if (scheme != "https" || host != "app.treefort.kids" || path != "/auth/confirmed") return Link.None
        val p = runCatching {
            query.orEmpty().split('&').associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('=', ""), "UTF-8") }
        }.getOrNull() ?: return Link.None
        return when (p["type"]) {
            "recovery" -> p["token_hash"]?.takeIf { it.isNotBlank() }?.let { Link.Recovery(it) } ?: Link.None
            "signup" -> Link.Signup
            else -> Link.None
        }
    }
}
