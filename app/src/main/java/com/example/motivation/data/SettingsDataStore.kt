package com.example.motivation.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    // --- Preference Keys ---
    private val selectedCategoriesKey = stringSetPreferencesKey("selected_categories")
    private val notificationContentTypeKey = stringPreferencesKey("notification_content_type")
    private val notificationModeKey = stringPreferencesKey("notification_mode")
    private val notificationIntervalMinutesKey = intPreferencesKey("notification_interval_minutes")
    private val notificationCountPerDayKey = intPreferencesKey("notification_count_per_day")
    private val streakReminderEnabledKey = booleanPreferencesKey("streak_reminder_enabled")
    private val streakCountKey = intPreferencesKey("streak_count")
    private val lastCompletionDateKey = longPreferencesKey("last_completion_date")
    private val graceDaysUsedThisWeekKey = intPreferencesKey("grace_days_used_this_week")
    private val weekStartDateKey = longPreferencesKey("week_start_date")
    private val unlockedAchievementsKey = stringSetPreferencesKey("unlocked_achievements")
    private val userNameKey = stringPreferencesKey("user_name")
    private val quietHoursEnabledKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStartKey = intPreferencesKey("quiet_hours_start")
    private val quietHoursEndKey = intPreferencesKey("quiet_hours_end")

    // --- Flows for observing data ---
    val selectedCategories: Flow<Set<String>> = dataStore.data.map { it[selectedCategoriesKey] ?: emptySet() }
    val notificationContentType: Flow<String> = dataStore.data.map { it[notificationContentTypeKey] ?: "Affirmation" }
    val notificationMode: Flow<String> = dataStore.data.map { it[notificationModeKey] ?: "DailyCount" }
    val notificationIntervalMinutes: Flow<Int> = dataStore.data.map { it[notificationIntervalMinutesKey] ?: 60 }
    val notificationCountPerDay: Flow<Int> = dataStore.data.map { it[notificationCountPerDayKey] ?: 1 }
    val streakReminderEnabled: Flow<Boolean> = dataStore.data.map { it[streakReminderEnabledKey] ?: true }
    val streakCount: Flow<Int> = dataStore.data.map { it[streakCountKey] ?: 0 }
    val lastCompletionDate: Flow<Long> = dataStore.data.map { it[lastCompletionDateKey] ?: 0L }
    val graceDaysUsedThisWeek: Flow<Int> = dataStore.data.map { it[graceDaysUsedThisWeekKey] ?: 0 }
    val weekStartDate: Flow<Long> = dataStore.data.map { it[weekStartDateKey] ?: 0L }
    val unlockedAchievements: Flow<Set<String>> = dataStore.data.map { it[unlockedAchievementsKey] ?: emptySet() }
    val userName: Flow<String> = dataStore.data.map { it[userNameKey] ?: "" }
    val quietHoursEnabled: Flow<Boolean> = dataStore.data.map { it[quietHoursEnabledKey] ?: false }
    val quietHoursStart: Flow<Int> = dataStore.data.map { it[quietHoursStartKey] ?: 1320 } // 10 PM
    val quietHoursEnd: Flow<Int> = dataStore.data.map { it[quietHoursEndKey] ?: 240 }    // 4 AM

    // --- Methods for updating data ---
    suspend fun setUserName(name: String) {
        dataStore.edit { it[userNameKey] = name }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { it[quietHoursEnabledKey] = enabled }
    }

    suspend fun setQuietHoursTime(startMinutes: Int, endMinutes: Int) {
        dataStore.edit {
            it[quietHoursStartKey] = startMinutes
            it[quietHoursEndKey] = endMinutes
        }
    }

    suspend fun setNotificationContentType(type: String) {
        dataStore.edit { it[notificationContentTypeKey] = type }
    }

    suspend fun setNotificationMode(mode: String) {
        dataStore.edit { it[notificationModeKey] = mode }
    }

    suspend fun setFrequencySettings(interval: Int) {
        dataStore.edit { it[notificationIntervalMinutesKey] = interval }
    }

    suspend fun setDailyCountSettings(count: Int) {
        dataStore.edit { it[notificationCountPerDayKey] = count }
    }

    suspend fun setStreakReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[streakReminderEnabledKey] = enabled }
    }

    suspend fun saveStreakData(count: Int, lastCompletion: Long, graceDays: Int, weekStart: Long) {
        dataStore.edit {
            it[streakCountKey] = count
            it[lastCompletionDateKey] = lastCompletion
            it[graceDaysUsedThisWeekKey] = graceDays
            it[weekStartDateKey] = weekStart
        }
    }

    suspend fun saveUnlockedAchievements(achievementIds: Set<String>) {
        dataStore.edit { it[unlockedAchievementsKey] = achievementIds }
    }
}
