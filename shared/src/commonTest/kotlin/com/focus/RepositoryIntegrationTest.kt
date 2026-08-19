package com.focus

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.focus.data.repository.*
import com.focus.database.FocusDatabase
import com.focus.model.JournalEntry
import com.focus.model.MoodEntry
import com.focus.model.Quote
import com.focus.sync.SqlDelightSyncQueue
import com.focus.sync.SyncOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoryIntegrationTest {

    private lateinit var database: FocusDatabase
    private lateinit var moodRepo: MoodRepository
    private lateinit var journalRepo: JournalRepository
    private lateinit var privateJournalRepo: PrivateJournalRepository
    private lateinit var quoteRepo: QuoteRepository
    private lateinit var achievementRepo: AchievementRepository
    private lateinit var syncQueue: SqlDelightSyncQueue

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FocusDatabase.Schema.create(driver)
        database = FocusDatabase(driver)

        moodRepo = SqlDelightMoodRepository(database, Dispatchers.Unconfined)
        journalRepo = SqlDelightJournalRepository(database, Dispatchers.Unconfined)
        privateJournalRepo = SqlDelightPrivateJournalRepository(database, Dispatchers.Unconfined)
        quoteRepo = SqlDelightQuoteRepository(database, Dispatchers.Unconfined)
        achievementRepo = SqlDelightAchievementRepository(database, Dispatchers.Unconfined)
        syncQueue = SqlDelightSyncQueue(database, Dispatchers.Unconfined)
    }

    @Test
    fun testMoodRepositoryInsertAndQuery() = runTest {
        moodRepo.insertMoodEntry("Great", "🔥", 5, 1000L)
        moodRepo.insertMoodEntry("Good", "😊", 4, 2000L)

        val moods = moodRepo.getAllMoodEntries().first()
        assertEquals(2, moods.size)
        assertEquals("Good", moods[0].moodName) // Descending order by timestamp

        val direct = moodRepo.getAllMoodEntriesDirect()
        assertEquals(2, direct.size)
    }

    @Test
    fun testJournalRepositoryInsertAndQuery() = runTest {
        val entry1 = JournalEntry("id-1", "First reflection", 1000L, "Today")
        val entry2 = JournalEntry("id-2", "Second reflection", 2000L, "Today")
        journalRepo.insertJournalEntry(entry1)
        journalRepo.insertJournalEntry(entry2)

        val list = journalRepo.getAllJournalEntries().first()
        assertEquals(2, list.size)
        assertEquals("id-2", list[0].id)

        val latest = journalRepo.getLatestJournalEntry()
        assertEquals("id-2", latest?.id)
    }

    @Test
    fun testPrivateJournalRepositoryOperations() = runTest {
        privateJournalRepo.insertPrivateEntry("Secret thoughts", 1000L, 2)
        val list = privateJournalRepo.getAllPrivateEntries().first()
        assertEquals(1, list.size)
        assertEquals("Secret thoughts", list[0].content)
    }

    @Test
    fun testQuoteRepositoryFavoritesAndHistory() = runTest {
        val quote = Quote("Consistency is key", "Motivation")
        quoteRepo.addFavorite(quote)

        val favs = quoteRepo.allFavorites.first()
        assertEquals(1, favs.size)
        assertEquals("Consistency is key", favs[0].text)

        val isFav = quoteRepo.isFavorite(quote.text).first()
        assertTrue(isFav)

        quoteRepo.addToHistory(quote)
        val history = quoteRepo.recentHistory.first()
        assertEquals(1, history.size)
    }

    @Test
    fun testAchievementEvaluation() = runTest {
        achievementRepo.initDefaultAchievements()
        val initial = achievementRepo.getAllAchievementsDirect()
        assertEquals(10, initial.size)

        var unlockedTitle: String? = null
        achievementRepo.checkAchievements(
            streakCount = 7,
            moodCount = 50,
            journalCount = 10,
            earlyBirdCount = 0,
            nightOwlCount = 0
        ) { unlocked ->
            if (unlocked.achievementId == "week_warrior") {
                unlockedTitle = unlocked.title
            }
        }

        val updated = achievementRepo.getAllAchievementsDirect().associateBy { it.achievementId }
        assertTrue(updated["week_warrior"]?.isUnlocked == true)
        assertTrue(updated["mood_tracker"]?.isUnlocked == true)
        assertEquals("Week Warrior", unlockedTitle)
    }

    @Test
    fun testSyncQueueOperations() = runTest {
        syncQueue.enqueue("journal", "id-123", SyncOperation.INSERT, "{\"text\":\"Hello\"}")
        val pending = syncQueue.getPendingItems()
        assertEquals(1, pending.size)
        assertEquals("journal", pending[0].entityType)

        syncQueue.markCompleted(pending[0].id)
        val afterComplete = syncQueue.getPendingItems()
        assertEquals(0, afterComplete.size)
    }
}
