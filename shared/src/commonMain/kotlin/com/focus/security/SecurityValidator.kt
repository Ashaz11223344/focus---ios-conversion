package com.focus.security

expect class SecurityValidator {
    fun isDeviceCompromised(): Boolean
}
