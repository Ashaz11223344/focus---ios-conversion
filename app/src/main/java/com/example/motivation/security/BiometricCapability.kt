package com.example.motivation.security

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK

object BiometricCapability {
    enum class BiometricType {
        FINGERPRINT,
        FACE,
        FINGERPRINT_AND_FACE,
        NONE
    }

    enum class BiometricStrength {
        STRONG,
        WEAK,
        NONE
    }

    private var cachedStrength: BiometricStrength? = null

    fun getAvailableStrength(context: Context): BiometricStrength {
        cachedStrength?.let { return it }

        val biometricManager = BiometricManager.from(context)

        // Try STRONG first
        val canStrong = try {
            biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        } catch (e: Exception) {
            Log.e("BiometricCapability", "Error checking BIOMETRIC_STRONG", e)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        }

        if (canStrong == BiometricManager.BIOMETRIC_SUCCESS) {
            cachedStrength = BiometricStrength.STRONG
            return BiometricStrength.STRONG
        }

        // Fallback to WEAK (covers face unlock on many devices)
        val canWeak = try {
            biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        } catch (e: Exception) {
            Log.e("BiometricCapability", "Error checking BIOMETRIC_WEAK", e)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        }

        if (canWeak == BiometricManager.BIOMETRIC_SUCCESS) {
            cachedStrength = BiometricStrength.WEAK
            return BiometricStrength.WEAK
        }

        Log.d("BiometricCapability", "canAuthStrong=$canStrong, canAuthWeak=$canWeak — no biometric available")
        cachedStrength = BiometricStrength.NONE
        return BiometricStrength.NONE
    }

    fun getAvailableBiometricType(context: Context): BiometricType {
        val strength = getAvailableStrength(context)
        if (strength == BiometricStrength.NONE) return BiometricType.NONE

        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)
        } else {
            false
        }

        return when {
            hasFingerprint && hasFace -> BiometricType.FINGERPRINT_AND_FACE
            hasFingerprint -> BiometricType.FINGERPRINT
            hasFace -> BiometricType.FACE
            // Device has biometric but doesn't report a specific feature flag
            // (common on many phones) — default to FINGERPRINT to show the icon
            else -> BiometricType.FINGERPRINT
        }
    }
}
