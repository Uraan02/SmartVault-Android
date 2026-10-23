package com.smartvault.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class SecurityManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_vault_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "sec_pin_hash"
        private const val KEY_PIN_SALT = "sec_pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "sec_biometric_enabled"
        private const val KEY_SCREENSHOT_PROTECTION = "sec_screenshot_prot"
        private const val KEY_AUTO_LOCK_TIMEOUT_MS = "sec_auto_lock_timeout"
        private const val KEY_PRIVACY_MODE = "sec_privacy_mode"
        private const val KEY_SECURITY_QUESTION = "sec_security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "sec_security_answer_hash"
        private const val KEY_APP_CURRENCY = "sec_app_currency"
        private const val KEY_ALLOW_NEGATIVE_BALANCE = "sec_allow_neg_balance"

        private const val DEFAULT_AUTO_LOCK_MS = 60_000L // 1 minute default
    }

    private var lastBackgroundTime: Long = 0
    private var isAppUnlocked: Boolean = false

    fun isPinSetup(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun setupPin(pin: String, securityQuestion: String? = null, securityAnswer: String? = null) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltString = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)

        val editor = prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, saltString)

        if (!securityQuestion.isNullOrBlank() && !securityAnswer.isNullOrBlank()) {
            val answerHash = hashText(securityAnswer.trim().lowercase())
            editor.putString(KEY_SECURITY_QUESTION, securityQuestion)
            editor.putString(KEY_SECURITY_ANSWER_HASH, answerHash)
        }
        editor.apply()
        isAppUnlocked = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltStr = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = Base64.decode(storedSaltStr, Base64.NO_WRAP)
        val testHash = hashPin(pin, salt)
        val matches = storedHash == testHash
        if (matches) {
            isAppUnlocked = true
        }
        return matches
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val storedAnswerHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false
        return storedAnswerHash == hashText(answer.trim().lowercase())
    }

    fun getSecurityQuestion(): String? {
        return prefs.getString(KEY_SECURITY_QUESTION, null)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isScreenshotProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, true) // ON by default
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTION, enabled).apply()
    }

    fun isPrivacyModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_MODE, false)
    }

    fun setPrivacyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
    }

    fun togglePrivacyMode(): Boolean {
        val newVal = !isPrivacyModeEnabled()
        setPrivacyModeEnabled(newVal)
        return newVal
    }

    fun getAutoLockTimeoutMs(): Long {
        return prefs.getLong(KEY_AUTO_LOCK_TIMEOUT_MS, DEFAULT_AUTO_LOCK_MS)
    }

    fun setAutoLockTimeoutMs(timeoutMs: Long) {
        prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT_MS, timeoutMs).apply()
    }

    fun getDefaultCurrency(): String {
        return prefs.getString(KEY_APP_CURRENCY, "PKR") ?: "PKR"
    }

    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString(KEY_APP_CURRENCY, currency).apply()
    }

    fun isNegativeBalanceAllowed(): Boolean {
        return prefs.getBoolean(KEY_ALLOW_NEGATIVE_BALANCE, true)
    }

    fun setNegativeBalanceAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_NEGATIVE_BALANCE, allowed).apply()
    }

    fun onAppPaused() {
        lastBackgroundTime = SystemClock.elapsedRealtime()
    }

    fun onAppResumed(): Boolean {
        if (!isPinSetup()) return true
        val timeout = getAutoLockTimeoutMs()
        val elapsed = SystemClock.elapsedRealtime() - lastBackgroundTime
        if (lastBackgroundTime > 0 && elapsed > timeout) {
            isAppUnlocked = false
        }
        return isAppUnlocked
    }

    fun lockApp() {
        isAppUnlocked = false
    }

    fun unlockApp() {
        isAppUnlocked = true
    }

    fun isUnlocked(): Boolean = isAppUnlocked

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun hashText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
