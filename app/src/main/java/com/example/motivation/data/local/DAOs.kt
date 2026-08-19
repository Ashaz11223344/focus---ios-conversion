package com.example.motivation.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MotivationDao {
    // Journal
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Update
    suspend fun updateJournalEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestJournalEntryDirect(): JournalEntryEntity?

    // Mood
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoodEntries(): Flow<List<MoodEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntryEntity)

    @Query("SELECT * FROM mood_entries WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getMoodLogsBetween(startDate: Long, endDate: Long): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMoodEntryForDayRange(startOfDay: Long, endOfDay: Long): MoodEntryEntity?

    @Query("DELETE FROM mood_entries WHERE id NOT IN (SELECT MAX(id) FROM mood_entries GROUP BY date(timestamp / 1000, 'unixepoch', 'localtime'))")
    suspend fun deleteDuplicateMoods()

    @Query("SELECT * FROM journal_entries WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getJournalEntriesBetween(startDate: Long, endDate: Long): List<JournalEntryEntity>

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteQuoteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteQuoteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE text = :quoteText)")
    fun isFavorite(quoteText: String): Flow<Boolean>

    // History
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<QuoteHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: QuoteHistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history WHERE text = :quoteText")
    suspend fun deleteHistoryByText(quoteText: String)

    // --- Backup & Restore Direct Operations ---
    @Query("SELECT * FROM journal_entries")
    suspend fun getAllJournalEntriesDirect(): List<JournalEntryEntity>

    @Query("SELECT * FROM mood_entries")
    suspend fun getAllMoodEntriesDirect(): List<MoodEntryEntity>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavoritesDirect(): List<FavoriteQuoteEntity>

    @Query("SELECT * FROM history")
    suspend fun getAllHistoryDirect(): List<QuoteHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntries(entries: List<JournalEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntries(entries: List<MoodEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(entries: List<FavoriteQuoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entries: List<QuoteHistoryEntity>)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllJournalEntries()

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAllMoodEntries()

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()

    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
}
