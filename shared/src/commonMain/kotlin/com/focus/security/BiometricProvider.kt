package com.focus.security

enum class BiometricStatus {
    AVAILABLE,
    NOT_AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE
}

expect class BiometricProvider {
    fun checkBiometricStatus(): BiometricStatus
    suspend fun authenticate(title: String, subtitle: String): Result<Boolean>
}
