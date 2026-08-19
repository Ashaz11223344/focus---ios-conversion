package com.example.motivation.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllSessionsDirect(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<FocusSessionEntity>)

    @Query("SELECT COUNT(*) FROM focus_sessions")
    suspend fun getSessionsCount(): Int

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getSessionsCountInTimeRange(startTime: Long, endTime: Long): Int

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}
