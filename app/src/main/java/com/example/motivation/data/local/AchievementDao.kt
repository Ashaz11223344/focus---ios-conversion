package com.example.motivation.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsDirect(): List<Achievement>

    @Query("SELECT * FROM achievements WHERE achievementId = :id LIMIT 1")
    suspend fun getAchievementById(id: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getAchievementsCount(): Int

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()
}
