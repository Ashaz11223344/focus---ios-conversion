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
import kotlinx.coroutines.flow.first
import com.example.motivation.model.SettingsBackup

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val dataStore = context.dataStore

    // --- Preference Keys ---
    private val selectedCategoriesKey = stringSetPreferencesKey("selected_categories")
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
    private val moodReminderEnabledKey = booleanPreferencesKey("mood_reminder_enabled")
    private val moodReminderTimeKey = intPreferencesKey("mood_reminder_time")
    private val quoteNotificationsEnabledKey = booleanPreferencesKey("quote_notifications_enabled")
    private val quoteScheduleTypeKey = stringPreferencesKey("quote_schedule_type")
    private val quoteTimeSlot1Key = intPreferencesKey("quote_time_slot_1")
    private val quoteTimeSlot2Key = intPreferencesKey("quote_time_slot_2")
    private val quoteTimeSlot3Key = intPreferencesKey("quote_time_slot_3")
    private val dndActiveKey = booleanPreferencesKey("dnd_currently_active")
    private val appBlockerConsentGrantedKey = booleanPreferencesKey("app_blocker_consent_granted")
    private val selectedGenreCategoriesKey = stringPreferencesKey("selected_genre_categories")
    private val genreRotationPointerKey = intPreferencesKey("genre_rotation_pointer")
    private val genreRotationLastDateKey = stringPreferencesKey("genre_rotation_last_date")
    private val userIdKey = stringPreferencesKey("user_id")
    private val userProfilePhotoUriKey = stringPreferencesKey("user_profile_photo_uri")
    private val enableBadgeDisplayKey = booleanPreferencesKey("enable_badge_display")
    private val profileCreatedDateKey = longPreferencesKey("profile_created_date")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    // --- Flows for observing data ---
    val userId: Flow<String> = dataStore.data.map { it[userIdKey] ?: "" }
    val userProfilePhotoUri: Flow<String?> = dataStore.data.map { it[userProfilePhotoUriKey] }
    val enableBadgeDisplay: Flow<Boolean> = dataStore.data.map { it[enableBadgeDisplayKey] ?: true }
    val profileCreatedDate: Flow<Long> = dataStore.data.map { it[profileCreatedDateKey] ?: 0L }
    val themeMode: Flow<com.example.motivation.ui.theme.ThemeMode> = dataStore.data.map { preferences ->
        val modeString = preferences[themeModeKey] ?: com.example.motivation.ui.theme.ThemeMode.DARK.name
        try {
            com.example.motivation.ui.theme.ThemeMode.valueOf(modeString)
        } catch (e: Exception) {
            com.example.motivation.ui.theme.ThemeMode.DARK
        }
    }
    val selectedCategories: Flow<Set<String>> = dataStore.data.map { it[selectedCategoriesKey] ?: emptySet() }
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
    val moodReminderEnabled: Flow<Boolean> = dataStore.data.map { it[moodReminderEnabledKey] ?: false }
    val moodReminderTime: Flow<Int> = dataStore.data.map { it[moodReminderTimeKey] ?: 1200 } // 8 PM (20 * 60)
    val quoteNotificationsEnabled: Flow<Boolean> = dataStore.data.map { it[quoteNotificationsEnabledKey] ?: true }
    val quoteScheduleType: Flow<String> = dataStore.data.map { it[quoteScheduleTypeKey] ?: "Random" }
    val quoteTimeSlot1: Flow<Int> = dataStore.data.map { it[quoteTimeSlot1Key] ?: -1 }
    val quoteTimeSlot2: Flow<Int> = dataStore.data.map { it[quoteTimeSlot2Key] ?: -1 }
    val quoteTimeSlot3: Flow<Int> = dataStore.data.map { it[quoteTimeSlot3Key] ?: -1 }
    val isDndActive: Flow<Boolean> = dataStore.data.map { it[dndActiveKey] ?: false }
    val appBlockerConsentGranted: Flow<Boolean> = dataStore.data.map { it[appBlockerConsentGrantedKey] ?: false }
    val selectedGenreCategories: Flow<String> = dataStore.data.map { it[selectedGenreCategoriesKey] ?: "" }
    val genreRotationPointer: Flow<Int> = dataStore.data.map { it[genreRotationPointerKey] ?: 0 }
    val genreRotationLastDate: Flow<String> = dataStore.data.map { it[genreRotationLastDateKey] ?: "" }

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

    suspend fun setMoodReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[moodReminderEnabledKey] = enabled }
    }

    suspend fun setMoodReminderTime(timeMinutes: Int) {
        dataStore.edit { it[moodReminderTimeKey] = timeMinutes }
    }

    suspend fun setNotificationMode(mode: String) {
        dataStore.edit { it[notificationModeKey] = mode }
    }

    suspend fun setFrequencySettings(interval: Int) {
        dataStore.edit { it[notificationIntervalMinutesKey] = interval }
    }

    suspend fun setDailyCountSettings(count: Int) {
        dataStore.edit {
            it[notificationCountPerDayKey] = count
            it[genreRotationPointerKey] = 0 // Reset pointer on QuotesPerDay changes
        }
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

    suspend fun setQuoteNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[quoteNotificationsEnabledKey] = enabled }
    }

    suspend fun setQuoteScheduleType(type: String) {
        dataStore.edit { it[quoteScheduleTypeKey] = type }
    }

    suspend fun setQuoteTimeSlot(slotIndex: Int, timeMinutes: Int) {
        dataStore.edit {
            when (slotIndex) {
                1 -> it[quoteTimeSlot1Key] = timeMinutes
                2 -> it[quoteTimeSlot2Key] = timeMinutes
                3 -> it[quoteTimeSlot3Key] = timeMinutes
            }
        }
    }

    suspend fun setDndActive(active: Boolean) {
        dataStore.edit { it[dndActiveKey] = active }
    }

    suspend fun setAppBlockerConsentGranted(granted: Boolean) {
        dataStore.edit { it[appBlockerConsentGrantedKey] = granted }
    }

    suspend fun setSelectedGenreCategories(categories: String) {
        dataStore.edit { it[selectedGenreCategoriesKey] = categories }
    }

    suspend fun setGenreRotationPointer(pointer: Int) {
        dataStore.edit { it[genreRotationPointerKey] = pointer }
    }

    suspend fun setGenreRotationLastDate(dateStr: String) {
        dataStore.edit { it[genreRotationLastDateKey] = dateStr }
    }

    suspend fun ensureUserId(): String {
        val prefs = dataStore.data.first()
        val existing = prefs[userIdKey]
        if (existing.isNullOrBlank()) {
            val newId = java.util.UUID.randomUUID().toString()
            dataStore.edit { it[userIdKey] = newId }
            return newId
        }
        return existing
    }

    suspend fun setUserProfilePhotoUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) {
                prefs[userProfilePhotoUriKey] = uri
            } else {
                prefs.remove(userProfilePhotoUriKey)
            }
        }
    }

    suspend fun setEnableBadgeDisplay(enable: Boolean) {
        dataStore.edit { it[enableBadgeDisplayKey] = enable }
    }

    suspend fun setProfileCreatedDate(timestamp: Long) {
        dataStore.edit { it[profileCreatedDateKey] = timestamp }
    }

    suspend fun setThemeMode(mode: com.example.motivation.ui.theme.ThemeMode) {
        dataStore.edit { it[themeModeKey] = mode.name }
    }


    // --- Backup & Restore Direct Operations ---
// --- Backup & Restore Direct Operations ---
    suspend fun getSettingsForBackup(): SettingsBackup {
        val prefs = dataStore.data.first()
        return SettingsBackup(
            userName = prefs[userNameKey] ?: "",
            selectedCategories = prefs[selectedCategoriesKey]?.toList() ?: emptyList(),
            notificationMode = prefs[notificationModeKey] ?: "DailyCount",
            notificationIntervalMinutes = prefs[notificationIntervalMinutesKey] ?: 60,
            notificationCountPerDay = prefs[notificationCountPerDayKey] ?: 1,
            streakReminderEnabled = prefs[streakReminderEnabledKey] ?: true,
            streakCount = prefs[streakCountKey] ?: 0,
            lastCompletionDate = prefs[lastCompletionDateKey] ?: 0L,
            graceDaysUsedThisWeek = prefs[graceDaysUsedThisWeekKey] ?: 0,
            weekStartDate = prefs[weekStartDateKey] ?: 0L,
            unlockedAchievements = prefs[unlockedAchievementsKey]?.toList() ?: emptyList(),
            quietHoursEnabled = prefs[quietHoursEnabledKey] ?: false,
            quietHoursStart = prefs[quietHoursStartKey] ?: 1320,
            quietHoursEnd = prefs[quietHoursEndKey] ?: 240,
            moodReminderEnabled = prefs[moodReminderEnabledKey] ?: false,
            moodReminderTime = prefs[moodReminderTimeKey] ?: 1200,
            quoteNotificationsEnabled = prefs[quoteNotificationsEnabledKey] ?: true,
            quoteScheduleType = prefs[quoteScheduleTypeKey] ?: "Random",
            quoteTimeSlot1 = prefs[quoteTimeSlot1Key] ?: -1,
            quoteTimeSlot2 = prefs[quoteTimeSlot2Key] ?: -1,
            quoteTimeSlot3 = prefs[quoteTimeSlot3Key] ?: -1,
            isDndActive = prefs[dndActiveKey] ?: false,
            appBlockerConsentGranted = prefs[appBlockerConsentGrantedKey] ?: false,
            selectedGenreCategories = prefs[selectedGenreCategoriesKey] ?: "",
            genreRotationPointer = prefs[genreRotationPointerKey] ?: 0,
            genreRotationLastDate = prefs[genreRotationLastDateKey] ?: "",
            userId = prefs[userIdKey] ?: "",
            userProfilePhotoUri = prefs[userProfilePhotoUriKey],
            enableBadgeDisplay = prefs[enableBadgeDisplayKey] ?: true,
            profileCreatedDate = prefs[profileCreatedDateKey] ?: 0L,
            themeMode = prefs[themeModeKey] ?: com.example.motivation.ui.theme.ThemeMode.DARK.name,
            profilePhotoBase64 = null
        )
    }

    suspend fun restoreSettings(settings: SettingsBackup) {
        dataStore.edit { prefs ->
            prefs[userNameKey] = settings.userName
            prefs[selectedCategoriesKey] = settings.selectedCategories.toSet()
            prefs[notificationModeKey] = settings.notificationMode
            prefs[notificationIntervalMinutesKey] = settings.notificationIntervalMinutes
            prefs[notificationCountPerDayKey] = settings.notificationCountPerDay
            prefs[streakReminderEnabledKey] = settings.streakReminderEnabled
            prefs[streakCountKey] = settings.streakCount
            prefs[lastCompletionDateKey] = settings.lastCompletionDate
            prefs[graceDaysUsedThisWeekKey] = settings.graceDaysUsedThisWeek
            prefs[weekStartDateKey] = settings.weekStartDate
            prefs[unlockedAchievementsKey] = settings.unlockedAchievements.toSet()
            prefs[quietHoursEnabledKey] = settings.quietHoursEnabled
            prefs[quietHoursStartKey] = settings.quietHoursStart
            prefs[quietHoursEndKey] = settings.quietHoursEnd
            prefs[moodReminderEnabledKey] = settings.moodReminderEnabled
            prefs[moodReminderTimeKey] = settings.moodReminderTime
            prefs[quoteNotificationsEnabledKey] = settings.quoteNotificationsEnabled
            prefs[quoteScheduleTypeKey] = settings.quoteScheduleType
            prefs[quoteTimeSlot1Key] = settings.quoteTimeSlot1
            prefs[quoteTimeSlot2Key] = settings.quoteTimeSlot2
            prefs[quoteTimeSlot3Key] = settings.quoteTimeSlot3
            prefs[dndActiveKey] = settings.isDndActive
            prefs[appBlockerConsentGrantedKey] = settings.appBlockerConsentGranted
            prefs[selectedGenreCategoriesKey] = settings.selectedGenreCategories
            prefs[genreRotationPointerKey] = settings.genreRotationPointer
            prefs[genreRotationLastDateKey] = settings.genreRotationLastDate
            prefs[userIdKey] = settings.userId
            if (settings.userProfilePhotoUri != null) {
                prefs[userProfilePhotoUriKey] = settings.userProfilePhotoUri
            } else {
                prefs.remove(userProfilePhotoUriKey)
            }
            prefs[enableBadgeDisplayKey] = settings.enableBadgeDisplay
            prefs[profileCreatedDateKey] = settings.profileCreatedDate
            prefs[themeModeKey] = settings.themeMode
        }
    }
}
