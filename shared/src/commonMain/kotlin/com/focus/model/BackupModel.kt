package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsBackup(
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
    val quietHoursStart: Int = 1320,
    val quietHoursEnd: Int = 240,
    val moodReminderEnabled: Boolean = false,
    val moodReminderTime: Int = 1200,
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
    val userId: String = "",
    val userProfilePhotoUri: String? = null,
    val enableBadgeDisplay: Boolean = true,
    val profileCreatedDate: Long = 0L,
    val profilePhotoBase64: String? = null,
    val themeMode: String = "DARK",
    val onboardingCompleted: Boolean = false,
    val onboardingVersion: Int = 0,
    val quickWallpaperOnHold: Boolean = true,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val timestamp: Long,
    val journalEntries: List<JournalEntry> = emptyList(),
    val privateJournalEntries: List<PrivateJournalEntry> = emptyList(),
    val moodEntries: List<MoodEntry> = emptyList(),
    val favorites: List<Quote> = emptyList(),
    val history: List<Quote> = emptyList(),
    val dndSchedules: List<DndSchedule> = emptyList(),
    val appBlockRules: List<FocusGuardRule> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val focusSessions: List<FocusSession> = emptyList(),
    val settings: SettingsBackup = SettingsBackup()
)
