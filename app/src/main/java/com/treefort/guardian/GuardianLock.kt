package com.treefort.guardian

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Guardian Lock: a PIN that keeps a curious kid out of Settings on a guardian's unlocked phone.
 *
 * It is a convenience lock, not account security (a 4-8 digit PIN can always be guessed given
 * enough tries, which is why wrong tries back off). There is no default PIN: the lock is off
 * until a guardian chooses one. The PIN is stored only as a salted PBKDF2 hash; the account itself
 * is protected by the Supabase sign-in, not by this lock.
 */
object GuardianLock {
    private const val FILE = "treefort_guardian_lock"
    private const val ITERATIONS = 120_000
    const val MIN_DIGITS = 4
    const val MAX_DIGITS = 8

    /** True while Settings has been unlocked in this app session. Reset when leaving Settings or backgrounding. */
    var unlocked by mutableStateOf(false)
        private set

    sealed interface Result {
        data object Ok : Result
        data class Wrong(val triesLeft: Int) : Result
        data class LockedOut(val seconds: Int) : Result
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun algorithm() = if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"

    private fun hash(pin: String, salt: ByteArray, algorithm: String): ByteArray =
        SecretKeyFactory.getInstance(algorithm).generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)).encoded

    /** Earlier builds kept an unsalted SHA-256 in the shared prefs; it is discarded, not migrated. */
    private fun purgeLegacy(ctx: Context) {
        ctx.getSharedPreferences("treefort_prefs", Context.MODE_PRIVATE).edit()
            .remove("anti_kid_pin").remove("anti_kid_pin_hash").remove("is_pin_lock_enabled").apply()
    }

    fun isSet(ctx: Context): Boolean {
        purgeLegacy(ctx)
        return prefs(ctx).getString("hash", null) != null
    }

    fun validPin(pin: String) = pin.length in MIN_DIGITS..MAX_DIGITS && pin.all { it.isDigit() }

    fun set(ctx: Context, pin: String) {
        require(validPin(pin)) { "PIN must be $MIN_DIGITS to $MAX_DIGITS digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val alg = algorithm()
        prefs(ctx).edit()
            .putString("alg", alg)
            .putString("salt", salt.toHex())
            .putString("hash", hash(pin, salt, alg).toHex())
            .putInt("failures", 0).putLong("locked_until", 0L)
            .apply()
        unlocked = true
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        unlocked = true
    }

    fun verify(ctx: Context, pin: String, now: Long = System.currentTimeMillis()): Result {
        val p = prefs(ctx)
        val lockedUntil = p.getLong("locked_until", 0L)
        if (now < lockedUntil) return Result.LockedOut(((lockedUntil - now + 999) / 1000).toInt())
        val salt = p.getString("salt", null)?.fromHex()
        val stored = p.getString("hash", null)?.fromHex()
        val alg = p.getString("alg", null) ?: algorithm()
        if (salt == null || stored == null) return Result.Ok
        val ok = MessageDigest.isEqual(hash(pin, salt, alg), stored)
        if (ok) {
            p.edit().putInt("failures", 0).putLong("locked_until", 0L).apply()
            unlocked = true
            return Result.Ok
        }
        val failures = p.getInt("failures", 0) + 1
        val delay = backoffSeconds(failures)
        p.edit().putInt("failures", failures).putLong("locked_until", if (delay > 0) now + delay * 1000L else 0L).apply()
        return if (delay > 0) Result.LockedOut(delay) else Result.Wrong(FREE_TRIES - failures)
    }

    private const val FREE_TRIES = 5

    /** 5 free tries, then 30 s, doubling each further miss, capped at 15 minutes. */
    internal fun backoffSeconds(failures: Int): Int =
        if (failures < FREE_TRIES) 0 else minOf(30 shl (failures - FREE_TRIES).coerceAtMost(5), 900)

    fun relock() { unlocked = false }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.fromHex() = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
