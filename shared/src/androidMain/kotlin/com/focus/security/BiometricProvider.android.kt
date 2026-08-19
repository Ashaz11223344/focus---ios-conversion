package com.focus.security

import android.content.Context
import androidx.biometric.BiometricManager

actual class BiometricProvider(private val context: Context) {
    actual fun checkBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.NOT_AVAILABLE
        }
    }

    actual suspend fun authenticate(title: String, subtitle: String): Result<Boolean> {
        // Platform biometric prompt hook
        return if (checkBiometricStatus() == BiometricStatus.AVAILABLE) {
            Result.success(true)
        } else {
            Result.failure(IllegalStateException("Biometrics not available"))
        }
    }
}
