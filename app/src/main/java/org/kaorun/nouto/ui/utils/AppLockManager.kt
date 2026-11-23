package org.kaorun.nouto.ui.utils

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLockManager {
    private const val PREFS_NAME = "app_lock_prefs"
    const val KEY_ENABLED = "app_lock_enabled"
    const val KEY_BIOMETRIC = "app_lock_biometric"
    const val KEY_TIMEOUT = "app_lock_timeout"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"

    private const val HASH_ITERATIONS = 10_000
    private const val HASH_KEY_LENGTH = 256
    private const val BIOMETRIC_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

    private var isUnlocked = false
    private var backgroundAtMs = 0L
    private var suppressNextLock = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPin(context: Context): Boolean {
        val p = prefs(context)
        return !p.getString(KEY_PIN_HASH, null).isNullOrEmpty() &&
            !p.getString(KEY_PIN_SALT, null).isNullOrEmpty()
    }

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false) && hasPin(context)

    fun isBiometricEnabled(context: Context): Boolean =
        isLockEnabled(context) &&
            prefs(context).getBoolean(KEY_BIOMETRIC, false) &&
            isBiometricAvailable(context)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_BIOMETRIC, enabled) }
    }

    fun getLockTimeoutValue(context: Context): String =
        prefs(context).getString(KEY_TIMEOUT, "0") ?: "0"

    fun setLockTimeoutValue(context: Context, value: String) {
        prefs(context).edit { putString(KEY_TIMEOUT, value) }
    }

    fun getLockTimeoutMs(context: Context): Long =
        getLockTimeoutValue(context).toLongOrNull() ?: 0L

    fun canAuthenticateBiometric(context: Context): Int =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_AUTHENTICATORS)

    fun isBiometricAvailable(context: Context): Boolean =
        canAuthenticateBiometric(context) == BiometricManager.BIOMETRIC_SUCCESS

    fun savePin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs(context).edit {
            putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            putBoolean(KEY_ENABLED, true)
        }
        markUnlocked()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val saltB64 = p.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = p.getString(KEY_PIN_HASH, null) ?: return false
        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val expectedHash = runCatching { Base64.decode(hashB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actualHash = hashPin(pin, salt)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    fun disableLock(context: Context) {
        prefs(context).edit {
            putBoolean(KEY_ENABLED, false)
            putBoolean(KEY_BIOMETRIC, false)
            remove(KEY_PIN_HASH)
            remove(KEY_PIN_SALT)
        }
        markUnlocked()
    }

    fun shouldShowLockScreen(context: Context): Boolean =
        isLockEnabled(context) && !isUnlocked

    fun markUnlocked() {
        isUnlocked = true
        backgroundAtMs = 0L
    }

    fun onAppBackgrounded() {
        backgroundAtMs = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded(context: Context) {
        if (!isLockEnabled(context)) {
            markUnlocked()
            return
        }
        if (suppressNextLock) {
            suppressNextLock = false
            backgroundAtMs = 0L
            return
        }
        if (isUnlocked && backgroundAtMs > 0L) {
            val elapsed = SystemClock.elapsedRealtime() - backgroundAtMs
            if (elapsed >= getLockTimeoutMs(context)) {
                isUnlocked = false
            }
        }
        backgroundAtMs = 0L
    }

    fun suppressNextLockOnce() {
        suppressNextLock = true
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, HASH_ITERATIONS, HASH_KEY_LENGTH)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
