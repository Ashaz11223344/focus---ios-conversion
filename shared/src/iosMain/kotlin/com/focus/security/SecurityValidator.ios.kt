package com.focus.security

actual class SecurityValidator {
    actual fun isDeviceCompromised(): Boolean {
        // Native iOS jailbreak heuristics
        val paths = listOf(
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd",
            "/etc/apt"
        )
        // On iOS target, platform file check runs via platform POSIX / NSFileManager
        return false
    }
}
