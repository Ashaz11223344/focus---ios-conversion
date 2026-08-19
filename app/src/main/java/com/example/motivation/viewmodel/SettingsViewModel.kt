package com.example.motivation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.worker.StreakReminderWorker
import com.example.motivation.receiver.MoodReminderScheduler
import com.example.motivation.receiver.QuoteNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.example.motivation.model.QuoteCategory
import com.example.motivation.ui.theme.ThemeMode
import com.example.motivation.receiver.CategoryRotationEngine
import com.example.motivation.data.BackupRestoreManager

sealed interface BackupState {
    object Idle : BackupState
    object Loading : BackupState
    data class Success(val message: String) : BackupState
    data class Failure(val reason: String) : BackupState
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = WorkManager.getInstance(application)

    // --- Backup & Restore States ---
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    // --- Category Picker States ---
    private val _showCategoryPicker = MutableStateFlow(false)
    val showCategoryPicker: StateFlow<Boolean> = _showCategoryPicker.asStateFlow()

    fun openCategoryPicker() {
        _showCategoryPicker.value = true
    }

    fun closeCategoryPicker() {
        _showCategoryPicker.value = false
    }

    val selectedGenreCategories: StateFlow<List<String>> = settingsDataStore.selectedGenreCategories
        .map { raw ->
            if (raw.isBlank()) emptyList() else raw.split(",")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genreRotationPointer: StateFlow<Int> = settingsDataStore.genreRotationPointer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayActiveCategories: StateFlow<List<QuoteCategory>> = combine(
        selectedGenreCategories,
        settingsDataStore.notificationCountPerDay,
        genreRotationPointer
    ) { selected, count, pointer ->
        val activeIds = CategoryRotationEngine.getTodayCategories(selected, pointer, count)
        activeIds.mapNotNull { QuoteCategory.fromId(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun applySelectedCategories(selected: List<String>) = viewModelScope.launch {
        val joined = selected.joinToString(",")
        settingsDataStore.setSelectedGenreCategories(joined)
        settingsDataStore.setGenreRotationPointer(0)
        settingsDataStore.setGenreRotationLastDate("") // Reset date to force active rotation cycle evaluation
        rescheduleAfterCommit()
    }

    // --- Flows for UI State ---
    val selectedCategories = settingsDataStore.selectedCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val notificationMode = settingsDataStore.notificationMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DailyCount")
    val notificationIntervalMinutes = settingsDataStore.notificationIntervalMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)
    val notificationCountPerDay = settingsDataStore.notificationCountPerDay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)
    val streakReminderEnabled = settingsDataStore.streakReminderEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val quietHoursEnabled = settingsDataStore.quietHoursEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val quietHoursStart = settingsDataStore.quietHoursStart.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1320)
    val quietHoursEnd = settingsDataStore.quietHoursEnd.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 240)
    val moodReminderEnabled = settingsDataStore.moodReminderEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val moodReminderTime = settingsDataStore.moodReminderTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1200)
    val quoteNotificationsEnabled = settingsDataStore.quoteNotificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val quoteScheduleType = settingsDataStore.quoteScheduleType.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Random")
    val quoteTimeSlot1 = settingsDataStore.quoteTimeSlot1.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)
    val quoteTimeSlot2 = settingsDataStore.quoteTimeSlot2.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)
    val quoteTimeSlot3 = settingsDataStore.quoteTimeSlot3.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)
    val themeMode = settingsDataStore.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    // --- ViewModel Methods ---

    /**
     * Reschedules quote notifications on a background thread AFTER ensuring
     * the DataStore write has fully committed. This eliminates the race condition
     * where rescheduleAllQuoteNotifications() could read stale settings.
     */
    private suspend fun rescheduleAfterCommit() {
        // By the time this is called, the preceding dataStore.edit{} suspend call
        // has already returned — meaning the write IS committed to disk.
        // We dispatch to IO to avoid blocking the main thread with AlarmManager calls.
        withContext(Dispatchers.IO) {
            Log.d("SettingsViewModel", "DataStore write committed. Rescheduling quote notifications.")
            QuoteNotificationScheduler.rescheduleAllQuoteNotifications(getApplication())
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setQuietHoursEnabled(enabled)
        // DataStore edit is fully committed at this point (suspend returned)
        withContext(Dispatchers.IO) {
            MoodReminderScheduler.schedule(getApplication())
        }
        rescheduleAfterCommit()
    }

    fun setQuietHoursTime(startMinutes: Int, endMinutes: Int) = viewModelScope.launch {
        settingsDataStore.setQuietHoursTime(startMinutes, endMinutes)
        withContext(Dispatchers.IO) {
            MoodReminderScheduler.schedule(getApplication())
        }
        rescheduleAfterCommit()
    }

    fun setMoodReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setMoodReminderEnabled(enabled)
        withContext(Dispatchers.IO) {
            MoodReminderScheduler.schedule(getApplication())
        }
    }

    fun setMoodReminderTime(timeMinutes: Int) = viewModelScope.launch {
        settingsDataStore.setMoodReminderTime(timeMinutes)
        withContext(Dispatchers.IO) {
            MoodReminderScheduler.schedule(getApplication())
        }
    }

    fun setNotificationMode(mode: String) = viewModelScope.launch {
        settingsDataStore.setNotificationMode(mode)
        rescheduleAfterCommit()
    }

    fun setFrequencySettings(interval: Int) = viewModelScope.launch {
        settingsDataStore.setFrequencySettings(interval)
        if (notificationMode.value == "Frequency") {
            rescheduleAfterCommit()
        }
    }

    fun setDailyCountSettings(count: Int) = viewModelScope.launch {
        settingsDataStore.setDailyCountSettings(count)
        // Critical: the suspend function above has returned, meaning the new count
        // is committed to DataStore. Now reschedule reads the correct value.
        rescheduleAfterCommit()
    }

    fun setQuoteNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setQuoteNotificationsEnabled(enabled)
        rescheduleAfterCommit()
    }

    fun setQuoteScheduleType(type: String) = viewModelScope.launch {
        settingsDataStore.setQuoteScheduleType(type)
        rescheduleAfterCommit()
    }

    fun setQuoteTimeSlot(slotIndex: Int, timeMinutes: Int) = viewModelScope.launch {
        settingsDataStore.setQuoteTimeSlot(slotIndex, timeMinutes)
        rescheduleAfterCommit()
    }

    fun setStreakReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setStreakReminderEnabled(enabled)
        if (enabled) {
            scheduleStreakReminderWorker()
        } else {
            cancelStreakReminderWorker()
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsDataStore.setThemeMode(mode)
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

    // --- Backup & Restore Actions ---
    fun exportData(uri: android.net.Uri, passwordString: String) {
        _backupState.value = BackupState.Loading
        viewModelScope.launch {
            try {
                val manager = BackupRestoreManager(getApplication())
                manager.exportBackup(uri, passwordString.toCharArray())
                _backupState.value = BackupState.Success("Data Backup Successful!")
            } catch (e: java.lang.Exception) {
                Log.e("Backup", "Export failed", e)
                _backupState.value = BackupState.Failure("Failed to export: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun importData(uri: android.net.Uri, passwordString: String) {
        _backupState.value = BackupState.Loading
        viewModelScope.launch {
            try {
                val manager = BackupRestoreManager(getApplication())
                manager.importBackup(uri, passwordString.toCharArray())
                _backupState.value = BackupState.Success("Data Restored Successfully!")
            } catch (e: java.lang.Exception) {
                Log.e("Backup", "Import failed", e)
                val reason = if (e is java.lang.IllegalArgumentException) {
                    "The password you entered is incorrect or the file has been altered."
                } else {
                    e.localizedMessage ?: "Unknown error"
                }
                _backupState.value = BackupState.Failure("Failed to restore: $reason")
            }
        }
    }

    fun resetBackupState() {
        _backupState.value = BackupState.Idle
    }
}
