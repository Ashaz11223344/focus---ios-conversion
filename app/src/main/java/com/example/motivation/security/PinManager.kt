package com.example.motivation.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {
    private const val PREFS_FILE = "secure_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val PBKDF2_ITERATIONS = 210_000
    private const val SALT_LENGTH_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    private fun getSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        PREFS_FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isPinSet(context: Context): Boolean {
        return getSharedPrefs(context).contains(KEY_PIN_HASH)
    }

    fun setPin(context: Context, rawPin: String) {
        val hash = hashWithPbkdf2(rawPin)
        getSharedPrefs(context).edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val savedHash = getSharedPrefs(context).getString(KEY_PIN_HASH, null) ?: return false

        // Detect legacy unsalted SHA-256 hash (no ':' separator) and migrate
        if (!savedHash.contains(":")) {
            val legacyMatch = savedHash == legacyHashSha256(inputPin)
            if (legacyMatch) {
                // Transparently upgrade to PBKDF2
                setPin(context, inputPin)
            }
            return legacyMatch
        }

        // PBKDF2 verification
        val parts = savedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].hexToByteArray()
        val expectedHash = parts[1]
        val inputHash = pbkdf2Hash(inputPin, salt)
        return inputHash == expectedHash
    }

    fun clearPin(context: Context) {
        getSharedPrefs(context).edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun getPinHash(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_PIN_HASH, null)
    }

    fun setPinHash(context: Context, hash: String?) {
        if (hash == null) {
            getSharedPrefs(context).edit().remove(KEY_PIN_HASH).apply()
        } else {
            getSharedPrefs(context).edit().putString(KEY_PIN_HASH, hash).apply()
        }
    }

    /**
     * PBKDF2 with random salt. Returns "salt_hex:hash_hex".
     */
    private fun hashWithPbkdf2(input: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2Hash(input, salt)
        return salt.toHex() + ":" + hash
    }

    /**
     * Derives a PBKDF2WithHmacSHA256 key from input + salt.
     */
    private fun pbkdf2Hash(input: String, salt: ByteArray): String {
        val spec = PBEKeySpec(input.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return hash.toHex()
    }

    /**
     * Legacy SHA-256 hash for migration purposes only.
     */
    private fun legacyHashSha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
        }
        return data
    }
}

