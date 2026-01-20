package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.worker.MotivationNotificationWorker
import com.example.motivation.worker.StreakReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = WorkManager.getInstance(application)

    // --- Flows for UI State ---
    val selectedCategories = settingsDataStore.selectedCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val notificationContentType = settingsDataStore.notificationContentType.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Affirmation")
    val notificationMode = settingsDataStore.notificationMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DailyCount")
    val notificationIntervalMinutes = settingsDataStore.notificationIntervalMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)
    val notificationCountPerDay = settingsDataStore.notificationCountPerDay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    val streakReminderEnabled = settingsDataStore.streakReminderEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // --- ViewModel Methods ---
    fun setNotificationContentType(type: String) = viewModelScope.launch {
        settingsDataStore.setNotificationContentType(type)
        scheduleMotivationWorker()
    }

    fun setNotificationMode(mode: String) = viewModelScope.launch {
        settingsDataStore.setNotificationMode(mode)
        scheduleMotivationWorker()
    }

    fun setFrequencySettings(interval: Int) = viewModelScope.launch {
        settingsDataStore.setFrequencySettings(interval)
        if (notificationMode.value == "Frequency") {
            scheduleMotivationWorker()
        }
    }

    fun setDailyCountSettings(count: Int) = viewModelScope.launch {
        settingsDataStore.setDailyCountSettings(count)
        if (notificationMode.value == "DailyCount") {
            scheduleMotivationWorker()
        }
    }

    fun setStreakReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setStreakReminderEnabled(enabled)
        if (enabled) {
            scheduleStreakReminderWorker()
        } else {
            cancelStreakReminderWorker()
        }
    }
    
    private suspend fun scheduleMotivationWorker() {
        cancelWorkers("MotivationWork")
        if (notificationMode.value == "Frequency") {
            val interval = notificationIntervalMinutes.first().toLong()
            val workRequest = PeriodicWorkRequestBuilder<MotivationNotificationWorker>(maxOf(interval, 15L), TimeUnit.MINUTES).build()
            workManager.enqueueUniquePeriodicWork("MotivationWork", ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        } else {
            val count = notificationCountPerDay.first()
            val interval = 24L * 60 / count
            val workRequest = PeriodicWorkRequestBuilder<MotivationNotificationWorker>(maxOf(interval, 15L), TimeUnit.MINUTES).build()
            workManager.enqueueUniquePeriodicWork("MotivationWork", ExistingPeriodicWorkPolicy.REPLACE, workRequest)
        }
    }

    private fun scheduleStreakReminderWorker() {
        // Schedule to run once daily in the evening
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20) // 8 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("StreakReminderWork", ExistingPeriodicWorkPolicy.REPLACE, workRequest)
    }

    private fun cancelWorkers(workName: String) {
        workManager.cancelUniqueWork(workName)
    }

    private fun cancelStreakReminderWorker() {
        workManager.cancelUniqueWork("StreakReminderWork")
    }
}
