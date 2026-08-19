package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.model.Quote
import com.example.motivation.data.QuoteRepository
import com.example.motivation.data.AchievementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.motivation.worker.StreakReminderWorker
import kotlinx.coroutines.delay

data class StreakUiState(
    val requiredQuote: Quote = Quote("Choose growth over comfort.", "Motivation"),
    val isQuoteCompletedToday: Boolean = false,
    val streakCount: Int = 0,
    val showSuccessAnimation: Boolean = false
)

class StreakViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = WorkManager.getInstance(application)
    private val achievementRepository = AchievementRepository(application)

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val streak = settingsDataStore.streakCount.first()
            val lastCompletion = settingsDataStore.lastCompletionDate.first()
            val graceDaysUsed = settingsDataStore.graceDaysUsedThisWeek.first()
            val weekStart = settingsDataStore.weekStartDate.first()

            updateStreakStatus(streak, lastCompletion, graceDaysUsed, weekStart)
            
            _uiState.update {
                it.copy(
                    requiredQuote = getQuoteForToday(),
                    isQuoteCompletedToday = isToday(lastCompletion),
                    streakCount = settingsDataStore.streakCount.first()
                )
            }
            scheduleStreakReminderWorker()
        }
    }

    fun completeDailyQuote() {
        viewModelScope.launch {
            _uiState.update { it.copy(showSuccessAnimation = true) }
            delay(1500) // Wait for animation

            val streak = settingsDataStore.streakCount.first() + 1
            val now = System.currentTimeMillis()
            val graceDays = settingsDataStore.graceDaysUsedThisWeek.first()
            val weekStart = settingsDataStore.weekStartDate.first()

            settingsDataStore.saveStreakData(streak, now, graceDays, weekStart)

            // Log focus session and trigger achievement unlocks
            achievementRepository.logFocusSession()

            _uiState.update {
                it.copy(
                    isQuoteCompletedToday = true,
                    showSuccessAnimation = false,
                    streakCount = streak
                )
            }
        }
    }

    private fun getQuoteForToday(): Quote {
        QuoteRepository.initialize(getApplication())
        val quotes = QuoteRepository.getAllQuotes()
        if (quotes.isEmpty()) {
            return Quote("Believe you can and you're halfway there.", "Motivation")
        }
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val today = Calendar.getInstance()
        val other = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }

    private suspend fun updateStreakStatus(currentStreak: Int, lastCompletion: Long, graceDaysUsed: Int, weekStart: Long) {
        if (isToday(lastCompletion) || currentStreak == 0) return
        val now = System.currentTimeMillis()
        val daysSinceLastCompletion = TimeUnit.MILLISECONDS.toDays(now - lastCompletion)
        var newStreak = currentStreak
        var newGraceDays = graceDaysUsed
        var newWeekStart = weekStart
        if (now - weekStart > TimeUnit.DAYS.toMillis(7)) {
            newWeekStart = now
            newGraceDays = 0
        }
        if (daysSinceLastCompletion > 1) {
            val daysToPenalize = daysSinceLastCompletion - 1
            if (daysToPenalize > (1 - newGraceDays)) {
                newStreak = 0
            } else {
                newGraceDays += daysToPenalize.toInt()
            }
        }
        settingsDataStore.saveStreakData(newStreak, lastCompletion, newGraceDays, newWeekStart)
    }

    private fun scheduleStreakReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("StreakReminderWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }
}
