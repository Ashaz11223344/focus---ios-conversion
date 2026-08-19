package com.focus.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.focus.database.FocusDatabase
import com.focus.model.MoodEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface MoodRepository {
    fun getAllMoodEntries(): Flow<List<MoodEntry>>
    suspend fun insertMoodEntry(moodName: String, moodEmoji: String, moodValue: Int, timestamp: Long, id: Long = 0)
    suspend fun getMoodLogsBetween(startDate: Long, endDate: Long): List<MoodEntry>
    suspend fun getMoodEntryForDayRange(startOfDay: Long, endOfDay: Long): MoodEntry?
    suspend fun deleteDuplicateMoods()
    suspend fun deleteAllMoodEntries()
    suspend fun getAllMoodEntriesDirect(): List<MoodEntry>
    suspend fun insertMoodEntriesDirect(entries: List<MoodEntry>)
}

class SqlDelightMoodRepository(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MoodRepository {
    private val queries = database.focusDatabaseQueries

    override fun getAllMoodEntries(): Flow<List<MoodEntry>> {
        return queries.selectAllMoodEntries()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map { MoodEntry(it.id, it.moodName, it.moodEmoji, it.moodValue.toInt(), it.timestamp) }
            }
    }

    override suspend fun insertMoodEntry(
        moodName: String,
        moodEmoji: String,
        moodValue: Int,
        timestamp: Long,
        id: Long
    ) = withContext(ioDispatcher) {
        val entryId = if (id == 0L) null else id
        queries.insertMoodEntry(entryId, moodName, moodEmoji, moodValue.toLong(), timestamp)
    }

    override suspend fun getMoodLogsBetween(startDate: Long, endDate: Long): List<MoodEntry> = withContext(ioDispatcher) {
        queries.getMoodLogsBetween(startDate, endDate)
            .executeAsList()
            .map { MoodEntry(it.id, it.moodName, it.moodEmoji, it.moodValue.toInt(), it.timestamp) }
    }

    override suspend fun getMoodEntryForDayRange(startOfDay: Long, endOfDay: Long): MoodEntry? = withContext(ioDispatcher) {
        queries.getMoodEntryForDayRange(startOfDay, endOfDay)
            .executeAsOneOrNull()
            ?.let { MoodEntry(it.id, it.moodName, it.moodEmoji, it.moodValue.toInt(), it.timestamp) }
    }

    override suspend fun deleteDuplicateMoods() = withContext(ioDispatcher) {
        queries.deleteDuplicateMoods()
    }

    override suspend fun deleteAllMoodEntries() = withContext(ioDispatcher) {
        queries.deleteAllMoodEntries()
    }

    override suspend fun getAllMoodEntriesDirect(): List<MoodEntry> = withContext(ioDispatcher) {
        queries.selectAllMoodEntries()
            .executeAsList()
            .map { MoodEntry(it.id, it.moodName, it.moodEmoji, it.moodValue.toInt(), it.timestamp) }
    }

    override suspend fun insertMoodEntriesDirect(entries: List<MoodEntry>) = withContext(ioDispatcher) {
        database.transaction {
            entries.forEach {
                val entryId = if (it.id == 0L) null else it.id
                queries.insertMoodEntry(entryId, it.moodName, it.moodEmoji, it.moodValue.toLong(), it.timestamp)
            }
        }
    }
}
