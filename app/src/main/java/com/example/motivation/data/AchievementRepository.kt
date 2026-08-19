package com.example.motivation.data

import android.content.Context
import com.example.motivation.data.local.*
import com.example.motivation.helper.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AchievementRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val achievementDao = database.achievementDao()
    private val focusSessionDao = database.focusSessionDao()
    private val motivationDao = database.motivationDao()
    private val privateJournalDao = database.privateJournalDao()
    private val settingsDataStore = SettingsDataStore(context)
    private val notificationHelper = NotificationHelper(context)

    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()

    suspend fun initDefaultAchievements() {
        val count = achievementDao.getAchievementsCount()
        if (count == 0) {
            val defaults = listOf(
                Achievement(
                    achievementId = "first_step",
                    title = "First Step",
                    description = "Complete 1 focus session",
                    iconEmoji = "🚀",
                    tier = "bronze",
                    tierColor = "#CD7F32",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 1,
                    category = "sessions"
                ),
                Achievement(
                    achievementId = "week_warrior",
                    title = "Week Warrior",
                    description = "7-day focus streak",
                    iconEmoji = "🔥",
                    tier = "silver",
                    tierColor = "#C0C0C0",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 7,
                    category = "streak"
                ),
                Achievement(
                    achievementId = "month_master",
                    title = "Month Master",
                    description = "30-day focus streak",
                    iconEmoji = "⭐",
                    tier = "gold",
                    tierColor = "#FFD700",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 30,
                    category = "streak"
                ),
                Achievement(
                    achievementId = "year_legend",
                    title = "Year Legend",
                    description = "365-day focus streak",
                    iconEmoji = "👑",
                    tier = "platinum",
                    tierColor = "#E5E4E2",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 365,
                    category = "streak"
                ),
                Achievement(
                    achievementId = "mood_tracker",
                    title = "Mood Tracker",
                    description = "Record 50 mood entries",
                    iconEmoji = "😊",
                    tier = "silver",
                    tierColor = "#C0C0C0",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 50,
                    category = "mood"
                ),
                Achievement(
                    achievementId = "journal_keeper",
                    title = "Journal Keeper",
                    description = "Write 100 journal entries",
                    iconEmoji = "✍️",
                    tier = "gold",
                    tierColor = "#FFD700",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 100,
                    category = "journal"
                ),
                Achievement(
                    achievementId = "focus_ninja",
                    title = "Focus Ninja",
                    description = "Complete 100 focus sessions",
                    iconEmoji = "🥷",
                    tier = "gold",
                    tierColor = "#FFD700",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 100,
                    category = "sessions"
                ),
                Achievement(
                    achievementId = "night_owl",
                    title = "Night Owl",
                    description = "Focus sessions after 8 PM (10 times)",
                    iconEmoji = "🌙",
                    tier = "silver",
                    tierColor = "#C0C0C0",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 10,
                    category = "sessions"
                ),
                Achievement(
                    achievementId = "early_bird",
                    title = "Early Bird",
                    description = "Focus sessions before 7 AM (10 times)",
                    iconEmoji = "☀️",
                    tier = "silver",
                    tierColor = "#C0C0C0",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 10,
                    category = "sessions"
                ),
                Achievement(
                    achievementId = "mythic_master",
                    title = "Mythic Master",
                    description = "Unlock 5+ other badges",
                    iconEmoji = "🌟",
                    tier = "mythic",
                    tierColor = "#8B008B",
                    unlockedDate = null,
                    isUnlocked = false,
                    progressCurrent = 0,
                    progressTarget = 5,
                    category = "mythic"
                )
            )
            achievementDao.insertAchievements(defaults)
        }
    }

    suspend fun logFocusSession() {
        focusSessionDao.insertSession(FocusSessionEntity())
        checkAchievements()
    }

    suspend fun checkAchievements() {
        // First ensure achievements are initialized
        initDefaultAchievements()

        // Fetch current stats
        val streakCount = settingsDataStore.streakCount.first()
        val totalSessions = focusSessionDao.getSessionsCount()
        
        // Count Early Bird and Night Owl sessions
        val allSessions = focusSessionDao.getAllSessionsDirect()
        var earlyBirdCount = 0
        var nightOwlCount = 0
        
        val calendar = Calendar.getInstance()
        for (session in allSessions) {
            calendar.timeInMillis = session.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour < 7) {
                earlyBirdCount++
            } else if (hour >= 20) {
                nightOwlCount++
            }
        }

        val moodCount = motivationDao.getAllMoodEntriesDirect().size
        val journalCount = motivationDao.getAllJournalEntriesDirect().size + privateJournalDao.getAllPrivateEntriesDirect().size

        // Get all achievements
        val achievements = achievementDao.getAllAchievementsDirect().associateBy { it.achievementId }.toMutableMap()

        // Update counts and unlock checks
        updateProgressAndCheckUnlock(achievements, "first_step", totalSessions)
        updateProgressAndCheckUnlock(achievements, "week_warrior", streakCount)
        updateProgressAndCheckUnlock(achievements, "month_master", streakCount)
        updateProgressAndCheckUnlock(achievements, "year_legend", streakCount)
        updateProgressAndCheckUnlock(achievements, "mood_tracker", moodCount)
        updateProgressAndCheckUnlock(achievements, "journal_keeper", journalCount)
        updateProgressAndCheckUnlock(achievements, "focus_ninja", totalSessions)
        updateProgressAndCheckUnlock(achievements, "early_bird", earlyBirdCount)
        updateProgressAndCheckUnlock(achievements, "night_owl", nightOwlCount)

        // Mythic Master count (how many other achievements are unlocked)
        val otherUnlockedCount = achievements.values.count { it.achievementId != "mythic_master" && it.isUnlocked }
        updateProgressAndCheckUnlock(achievements, "mythic_master", otherUnlockedCount)

        // Save back
        val listToSave = achievements.values.toList()
        achievementDao.insertAchievements(listToSave)

        // Update settingsDataStore unlocked list for backwards compat
        val unlockedIds = listToSave.filter { it.isUnlocked }.map { it.achievementId }.toSet()
        settingsDataStore.saveUnlockedAchievements(unlockedIds)
    }

    private suspend fun updateProgressAndCheckUnlock(
        achievements: MutableMap<String, Achievement>,
        id: String,
        currentProgress: Int
    ) {
        val achievement = achievements[id] ?: return
        val newProgress = currentProgress.coerceAtMost(achievement.progressTarget)
        if (newProgress == achievement.progressCurrent && achievement.isUnlocked) return

        val willUnlock = newProgress >= achievement.progressTarget
        val wasUnlocked = achievement.isUnlocked

        val updated = achievement.copy(
            progressCurrent = newProgress,
            isUnlocked = willUnlock,
            unlockedDate = if (willUnlock && !wasUnlocked) System.currentTimeMillis() else achievement.unlockedDate
        )

        achievements[id] = updated

        if (willUnlock && !wasUnlocked) {
            notificationHelper.showAchievementNotification(updated)
        }
    }
}
