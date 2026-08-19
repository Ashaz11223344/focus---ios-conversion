package com.example.motivation.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateJournalDao {
    @Query("SELECT * FROM private_journal_entries ORDER BY timestamp DESC")
    fun getAllPrivateEntries(): Flow<List<PrivateJournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateEntry(entry: PrivateJournalEntry)

    @Delete
    suspend fun deletePrivateEntry(entry: PrivateJournalEntry)

    @Query("DELETE FROM private_journal_entries")
    suspend fun deleteAllPrivateEntries()

    @Query("SELECT * FROM private_journal_entries WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getPrivateEntriesBetween(startDate: Long, endDate: Long): List<PrivateJournalEntry>

    // --- Backup & Restore Direct Operations ---
    @Query("SELECT * FROM private_journal_entries")
    suspend fun getAllPrivateEntriesDirect(): List<PrivateJournalEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateEntries(entries: List<PrivateJournalEntry>)
}
