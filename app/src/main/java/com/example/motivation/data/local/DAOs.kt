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

    // Mood
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoodEntries(): Flow<List<MoodEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntryEntity)

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

    @Query("DELETE FROM history WHERE text = :quoteText")
    suspend fun deleteHistoryByText(quoteText: String)
}
