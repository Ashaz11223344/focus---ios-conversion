package com.focus.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.focus.database.FocusDatabase
import com.focus.model.Achievement
import com.focus.model.FocusSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AchievementRepository {
    val allAchievements: Flow<List<Achievement>>
    suspend fun initDefaultAchievements()
    suspend fun logFocusSession(timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
    suspend fun getSessionsCount(): Long
    suspend fun getAllSessionsDirect(): List<FocusSession>
    suspend fun getAllAchievementsDirect(): List<Achievement>
    suspend fun insertAchievementsDirect(achievements: List<Achievement>)
    suspend fun insertSessionsDirect(sessions: List<FocusSession>)
    suspend fun deleteAllAchievements()
    suspend fun deleteAllSessions()
    suspend fun checkAchievements(
        streakCount: Int,
        moodCount: Int,
        journalCount: Int,
        earlyBirdCount: Int,
        nightOwlCount: Int,
        onUnlock: ((Achievement) -> Unit)? = null
    )
}

class SqlDelightAchievementRepository(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AchievementRepository {
    private val queries = database.focusDatabaseQueries

    override val allAchievements: Flow<List<Achievement>> = queries.selectAllAchievements()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list ->
            list.map {
                Achievement(
                    achievementId = it.achievementId,
                    title = it.title,
                    description = it.description,
                    iconEmoji = it.iconEmoji,
                    tier = it.tier,
                    tierColor = it.tierColor,
                    unlockedDate = it.unlockedDate,
                    isUnlocked = it.isUnlocked == 1L,
                    progressCurrent = it.progressCurrent.toInt(),
                    progressTarget = it.progressTarget.toInt(),
                    category = it.category
                )
            }
        }

    override suspend fun initDefaultAchievements() = withContext(ioDispatcher) {
        val count = queries.getAchievementsCount().executeAsOne()
        if (count == 0L) {
            val defaults = listOf(
                Achievement("first_step", "First Step", "Complete 1 focus session", "🚀", "bronze", "#CD7F32", null, false, 0, 1, "sessions"),
                Achievement("week_warrior", "Week Warrior", "7-day focus streak", "🔥", "silver", "#C0C0C0", null, false, 0, 7, "streak"),
                Achievement("month_master", "Month Master", "30-day focus streak", "⭐", "gold", "#FFD700", null, false, 0, 30, "streak"),
                Achievement("year_legend", "Year Legend", "365-day focus streak", "👑", "platinum", "#E5E4E2", null, false, 0, 365, "streak"),
                Achievement("mood_tracker", "Mood Tracker", "Record 50 mood entries", "😊", "silver", "#C0C0C0", null, false, 0, 50, "mood"),
                Achievement("journal_keeper", "Journal Keeper", "Write 100 journal entries", "✍️", "gold", "#FFD700", null, false, 0, 100, "journal"),
                Achievement("focus_ninja", "Focus Ninja", "Complete 100 focus sessions", "🥷", "gold", "#FFD700", null, false, 0, 100, "sessions"),
                Achievement("night_owl", "Night Owl", "Focus sessions after 8 PM (10 times)", "🌙", "silver", "#C0C0C0", null, false, 0, 10, "sessions"),
                Achievement("early_bird", "Early Bird", "Focus sessions before 7 AM (10 times)", "☀️", "silver", "#C0C0C0", null, false, 0, 10, "sessions"),
                Achievement("mythic_master", "Mythic Master", "Unlock 5+ other badges", "🌟", "mythic", "#8B008B", null, false, 0, 5, "mythic")
            )
            insertAchievementsDirect(defaults)
        }
    }

    override suspend fun logFocusSession(timestamp: Long) = withContext(ioDispatcher) {
        queries.insertSession(timestamp)
    }

    override suspend fun getSessionsCount(): Long = withContext(ioDispatcher) {
        queries.getSessionsCount().executeAsOne()
    }

    override suspend fun getAllSessionsDirect(): List<FocusSession> = withContext(ioDispatcher) {
        queries.selectAllSessions().executeAsList().map { FocusSession(it.id, it.timestamp) }
    }

    override suspend fun getAllAchievementsDirect(): List<Achievement> = withContext(ioDispatcher) {
        queries.selectAllAchievements().executeAsList().map {
            Achievement(
                achievementId = it.achievementId,
                title = it.title,
                description = it.description,
                iconEmoji = it.iconEmoji,
                tier = it.tier,
                tierColor = it.tierColor,
                unlockedDate = it.unlockedDate,
                isUnlocked = it.isUnlocked == 1L,
                progressCurrent = it.progressCurrent.toInt(),
                progressTarget = it.progressTarget.toInt(),
                category = it.category
            )
        }
    }

    override suspend fun insertAchievementsDirect(achievements: List<Achievement>) = withContext(ioDispatcher) {
        database.transaction {
            achievements.forEach {
                queries.insertAchievement(
                    achievementId = it.achievementId,
                    title = it.title,
                    description = it.description,
                    iconEmoji = it.iconEmoji,
                    tier = it.tier,
                    tierColor = it.tierColor,
                    unlockedDate = it.unlockedDate,
                    isUnlocked = if (it.isUnlocked) 1L else 0L,
                    progressCurrent = it.progressCurrent.toLong(),
                    progressTarget = it.progressTarget.toLong(),
                    category = it.category
                )
            }
        }
    }

    override suspend fun insertSessionsDirect(sessions: List<FocusSession>) = withContext(ioDispatcher) {
        database.transaction {
            sessions.forEach {
                queries.insertSession(it.timestamp)
            }
        }
    }

    override suspend fun deleteAllAchievements() = withContext(ioDispatcher) {
        queries.deleteAllAchievements()
    }

    override suspend fun deleteAllSessions() = withContext(ioDispatcher) {
        queries.deleteAllSessions()
    }

    override suspend fun checkAchievements(
        streakCount: Int,
        moodCount: Int,
        journalCount: Int,
        earlyBirdCount: Int,
        nightOwlCount: Int,
        onUnlock: ((Achievement) -> Unit)?
    ) = withContext(ioDispatcher) {
        initDefaultAchievements()
        val totalSessions = getSessionsCount().toInt()
        val achievementsMap = getAllAchievementsDirect().associateBy { it.achievementId }.toMutableMap()

        fun updateProgressAndCheckUnlock(id: String, currentProgress: Int) {
            val achievement = achievementsMap[id] ?: return
            val newProgress = currentProgress.coerceAtMost(achievement.progressTarget)
            if (newProgress == achievement.progressCurrent && achievement.isUnlocked) return

            val willUnlock = newProgress >= achievement.progressTarget
            val wasUnlocked = achievement.isUnlocked

            val updated = achievement.copy(
                progressCurrent = newProgress,
                isUnlocked = willUnlock,
                unlockedDate = if (willUnlock && !wasUnlocked) kotlinx.datetime.Clock.System.now().toEpochMilliseconds() else achievement.unlockedDate
            )
            achievementsMap[id] = updated

            if (willUnlock && !wasUnlocked) {
                onUnlock?.invoke(updated)
            }
        }

        updateProgressAndCheckUnlock("first_step", totalSessions)
        updateProgressAndCheckUnlock("week_warrior", streakCount)
        updateProgressAndCheckUnlock("month_master", streakCount)
        updateProgressAndCheckUnlock("year_legend", streakCount)
        updateProgressAndCheckUnlock("mood_tracker", moodCount)
        updateProgressAndCheckUnlock("journal_keeper", journalCount)
        updateProgressAndCheckUnlock("focus_ninja", totalSessions)
        updateProgressAndCheckUnlock("early_bird", earlyBirdCount)
        updateProgressAndCheckUnlock("night_owl", nightOwlCount)

        val otherUnlockedCount = achievementsMap.values.count { it.achievementId != "mythic_master" && it.isUnlocked }
        updateProgressAndCheckUnlock("mythic_master", otherUnlockedCount)

        insertAchievementsDirect(achievementsMap.values.toList())
    }
}
