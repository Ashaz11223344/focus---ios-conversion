package com.example.motivation.ui.onboarding

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)

    private val _currentScreen = MutableStateFlow(0)
    val currentScreen: StateFlow<Int> = _currentScreen.asStateFlow()

    private var lastNextTapTime = 0L

    fun nextScreen() {
        val now = System.currentTimeMillis()
        if (now - lastNextTapTime < 300L) return // Debounce spam taps to prevent skip glitches
        lastNextTapTime = now

        if (_currentScreen.value < 7) {
            _currentScreen.value += 1
        }
    }

    fun prevScreen() {
        if (_currentScreen.value > 0) {
            _currentScreen.value -= 1
        }
    }

    fun skipToLastScreen() {
        _currentScreen.value = 7
    }

    fun completeOnboarding() {
        sharedPrefs.edit()
            .putBoolean("onboarding_completed", true)
            .putInt("onboarding_version", 1)
            .apply()
    }
}
