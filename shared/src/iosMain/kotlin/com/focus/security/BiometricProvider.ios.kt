package com.focus.security

actual class BiometricProvider {
    actual fun checkBiometricStatus(): BiometricStatus {
        return BiometricStatus.AVAILABLE
    }

    actual suspend fun authenticate(title: String, subtitle: String): Result<Boolean> {
        return Result.success(true)
    }
}
