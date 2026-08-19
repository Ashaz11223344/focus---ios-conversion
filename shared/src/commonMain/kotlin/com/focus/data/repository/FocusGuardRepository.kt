package com.focus.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.focus.database.FocusDatabase
import com.focus.model.DndSchedule
import com.focus.model.FocusGuardRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface FocusGuardRepository {
    val allDndSchedules: Flow<List<DndSchedule>>
    suspend fun getDndScheduleById(id: Long): DndSchedule?
    suspend fun insertDndSchedule(schedule: DndSchedule)
    suspend fun deleteDndSchedule(id: Long)
    suspend fun getAllDndSchedulesDirect(): List<DndSchedule>
    suspend fun insertDndSchedulesDirect(schedules: List<DndSchedule>)
    suspend fun deleteAllDndSchedules()

    val allAppBlockRules: Flow<List<FocusGuardRule>>
    suspend fun getEnabledAppBlockRules(): List<FocusGuardRule>
    suspend fun insertAppBlockRule(rule: FocusGuardRule)
    suspend fun deleteAppBlockRule(packageName: String)
    suspend fun getAllAppBlockRulesDirect(): List<FocusGuardRule>
    suspend fun insertAppBlockRulesDirect(rules: List<FocusGuardRule>)
    suspend fun deleteAllAppBlockRules()
}

class SqlDelightFocusGuardRepository(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FocusGuardRepository {
    private val queries = database.focusDatabaseQueries

    override val allDndSchedules: Flow<List<DndSchedule>> = queries.selectAllDndSchedules()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list ->
            list.map {
                DndSchedule(
                    id = it.id,
                    label = it.label,
                    startHour = it.startHour.toInt(),
                    startMinute = it.startMinute.toInt(),
                    endHour = it.endHour.toInt(),
                    endMinute = it.endMinute.toInt(),
                    daysOfWeek = it.daysOfWeek.toInt(),
                    isEnabled = it.isEnabled == 1L
                )
            }
        }

    override suspend fun getDndScheduleById(id: Long): DndSchedule? = withContext(ioDispatcher) {
        queries.getDndScheduleById(id).executeAsOneOrNull()?.let {
            DndSchedule(
                id = it.id,
                label = it.label,
                startHour = it.startHour.toInt(),
                startMinute = it.startMinute.toInt(),
                endHour = it.endHour.toInt(),
                endMinute = it.endMinute.toInt(),
                daysOfWeek = it.daysOfWeek.toInt(),
                isEnabled = it.isEnabled == 1L
            )
        }
    }

    override suspend fun insertDndSchedule(schedule: DndSchedule) = withContext(ioDispatcher) {
        val entryId = if (schedule.id == 0L) null else schedule.id
        queries.insertDndSchedule(
            entryId,
            schedule.label,
            schedule.startHour.toLong(),
            schedule.startMinute.toLong(),
            schedule.endHour.toLong(),
            schedule.endMinute.toLong(),
            schedule.daysOfWeek.toLong(),
            if (schedule.isEnabled) 1L else 0L
        )
    }

    override suspend fun deleteDndSchedule(id: Long) = withContext(ioDispatcher) {
        queries.deleteDndSchedule(id)
    }

    override suspend fun getAllDndSchedulesDirect(): List<DndSchedule> = withContext(ioDispatcher) {
        queries.selectAllDndSchedules().executeAsList().map {
            DndSchedule(
                id = it.id,
                label = it.label,
                startHour = it.startHour.toInt(),
                startMinute = it.startMinute.toInt(),
                endHour = it.endHour.toInt(),
                endMinute = it.endMinute.toInt(),
                daysOfWeek = it.daysOfWeek.toInt(),
                isEnabled = it.isEnabled == 1L
            )
        }
    }

    override suspend fun insertDndSchedulesDirect(schedules: List<DndSchedule>) = withContext(ioDispatcher) {
        database.transaction {
            schedules.forEach {
                val entryId = if (it.id == 0L) null else it.id
                queries.insertDndSchedule(
                    entryId,
                    it.label,
                    it.startHour.toLong(),
                    it.startMinute.toLong(),
                    it.endHour.toLong(),
                    it.endMinute.toLong(),
                    it.daysOfWeek.toLong(),
                    if (it.isEnabled) 1L else 0L
                )
            }
        }
    }

    override suspend fun deleteAllDndSchedules() = withContext(ioDispatcher) {
        queries.deleteAllDndSchedules()
    }

    override val allAppBlockRules: Flow<List<FocusGuardRule>> = queries.selectAllAppBlockRules()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list ->
            list.map {
                FocusGuardRule(
                    packageName = it.packageName,
                    appName = it.appName,
                    startHour = it.startHour.toInt(),
                    startMinute = it.startMinute.toInt(),
                    endHour = it.endHour.toInt(),
                    endMinute = it.endMinute.toInt(),
                    daysOfWeek = it.daysOfWeek.toInt(),
                    isEnabled = it.isEnabled == 1L
                )
            }
        }

    override suspend fun getEnabledAppBlockRules(): List<FocusGuardRule> = withContext(ioDispatcher) {
        queries.getEnabledAppBlockRules().executeAsList().map {
            FocusGuardRule(
                packageName = it.packageName,
                appName = it.appName,
                startHour = it.startHour.toInt(),
                startMinute = it.startMinute.toInt(),
                endHour = it.endHour.toInt(),
                endMinute = it.endMinute.toInt(),
                daysOfWeek = it.daysOfWeek.toInt(),
                isEnabled = it.isEnabled == 1L
            )
        }
    }

    override suspend fun insertAppBlockRule(rule: FocusGuardRule) = withContext(ioDispatcher) {
        queries.insertAppBlockRule(
            rule.packageName,
            rule.appName,
            rule.startHour.toLong(),
            rule.startMinute.toLong(),
            rule.endHour.toLong(),
            rule.endMinute.toLong(),
            rule.daysOfWeek.toLong(),
            if (rule.isEnabled) 1L else 0L
        )
    }

    override suspend fun deleteAppBlockRule(packageName: String) = withContext(ioDispatcher) {
        queries.deleteAppBlockRule(packageName)
    }

    override suspend fun getAllAppBlockRulesDirect(): List<FocusGuardRule> = withContext(ioDispatcher) {
        queries.selectAllAppBlockRules().executeAsList().map {
            FocusGuardRule(
                packageName = it.packageName,
                appName = it.appName,
                startHour = it.startHour.toInt(),
                startMinute = it.startMinute.toInt(),
                endHour = it.endHour.toInt(),
                endMinute = it.endMinute.toInt(),
                daysOfWeek = it.daysOfWeek.toInt(),
                isEnabled = it.isEnabled == 1L
            )
        }
    }

    override suspend fun insertAppBlockRulesDirect(rules: List<FocusGuardRule>) = withContext(ioDispatcher) {
        database.transaction {
            rules.forEach {
                queries.insertAppBlockRule(
                    it.packageName,
                    it.appName,
                    it.startHour.toLong(),
                    it.startMinute.toLong(),
                    it.endHour.toLong(),
                    it.endMinute.toLong(),
                    it.daysOfWeek.toLong(),
                    if (it.isEnabled) 1L else 0L
                )
            }
        }
    }

    override suspend fun deleteAllAppBlockRules() = withContext(ioDispatcher) {
        queries.deleteAllAppBlockRules()
    }
}
