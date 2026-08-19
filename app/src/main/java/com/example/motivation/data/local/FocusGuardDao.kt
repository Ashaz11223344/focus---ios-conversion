package com.example.motivation.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusGuardDao {

    // --- DND Schedules ---
    @Query("SELECT * FROM dnd_schedules ORDER BY startHour ASC")
    fun getAllDndSchedules(): Flow<List<DndScheduleEntity>>

    @Query("SELECT * FROM dnd_schedules WHERE id = :id")
    suspend fun getDndScheduleById(id: Int): DndScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDndSchedule(schedule: DndScheduleEntity): Long

    @Update
    suspend fun updateDndSchedule(schedule: DndScheduleEntity)

    @Delete
    suspend fun deleteDndSchedule(schedule: DndScheduleEntity)

    // --- App Block Rules ---
    @Query("SELECT * FROM app_block_rules ORDER BY appName ASC")
    fun getAllAppBlockRules(): Flow<List<AppBlockRuleEntity>>

    @Query("SELECT * FROM app_block_rules WHERE isEnabled = 1")
    suspend fun getEnabledAppBlockRules(): List<AppBlockRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppBlockRule(rule: AppBlockRuleEntity)

    @Update
    suspend fun updateAppBlockRule(rule: AppBlockRuleEntity)

    @Query("DELETE FROM app_block_rules WHERE packageName = :pkg")
    suspend fun deleteAppBlockRule(pkg: String)

    // --- Backup & Restore Direct Operations ---
    @Query("SELECT * FROM dnd_schedules")
    suspend fun getAllDndSchedulesDirect(): List<DndScheduleEntity>

    @Query("SELECT * FROM app_block_rules")
    suspend fun getAllAppBlockRulesDirect(): List<AppBlockRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDndSchedules(schedules: List<DndScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppBlockRules(rules: List<AppBlockRuleEntity>)

    @Query("DELETE FROM dnd_schedules")
    suspend fun deleteAllDndSchedules()

    @Query("DELETE FROM app_block_rules")
    suspend fun deleteAllAppBlockRules()
}
