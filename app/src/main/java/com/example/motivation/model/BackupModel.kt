package com.example.motivation.model

import com.example.motivation.data.local.*

/**
 * Maps all Jetpack DataStore preferences, SharedPreferences, and Secure Preferences (PIN Hash)
 * to a serializable model.
 */
data class SettingsBackup(
    // DataStore Configurations
    val userName: String = "",
    val selectedCategories: List<String> = emptyList(),
    val notificationMode: String = "DailyCount",
    val notificationIntervalMinutes: Int = 60,
    val notificationCountPerDay: Int = 1,
    val streakReminderEnabled: Boolean = true,
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0L,
    val graceDaysUsedThisWeek: Int = 0,
    val weekStartDate: Long = 0L,
    val unlockedAchievements: List<String> = emptyList(),
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 1320, // 10 PM
    val quietHoursEnd: Int = 240,    // 4 AM
    val moodReminderEnabled: Boolean = false,
    val moodReminderTime: Int = 1200, // 8 PM
    val quoteNotificationsEnabled: Boolean = true,
    val quoteScheduleType: String = "Random",
    val quoteTimeSlot1: Int = -1,
    val quoteTimeSlot2: Int = -1,
    val quoteTimeSlot3: Int = -1,
    val isDndActive: Boolean = false,
    val appBlockerConsentGranted: Boolean = false,
    val selectedGenreCategories: String = "",
    val genreRotationPointer: Int = 0,
    val genreRotationLastDate: String = "",
    
    // User Profile & New Configs
    val userId: String = "",
    val userProfilePhotoUri: String? = null,
    val enableBadgeDisplay: Boolean = true,
    val profileCreatedDate: Long = 0L,
    val profilePhotoBase64: String? = null,
    val themeMode: String = "DARK",

    // Standard SharedPreferences ("motivation_prefs")
    val onboardingCompleted: Boolean = false,
    val onboardingVersion: Int = 0,
    val quickWallpaperOnHold: Boolean = true,

    // Secure EncryptedSharedPreferences ("secure_prefs")
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false
)

/**
 * Unified backup payload matching all Room Database tables and general preferences.
 */
data class BackupPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Room tables
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val privateJournalEntries: List<PrivateJournalEntry> = emptyList(),
    val moodEntries: List<MoodEntryEntity> = emptyList(),
    val favorites: List<FavoriteQuoteEntity> = emptyList(),
    val history: List<QuoteHistoryEntity> = emptyList(),
    val dndSchedules: List<DndScheduleEntity> = emptyList(),
    val appBlockRules: List<AppBlockRuleEntity> = emptyList(),
    val achievements: List<com.example.motivation.data.local.Achievement> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),

    // Shared and DataStore settings
    val settings: SettingsBackup = SettingsBackup()
)
