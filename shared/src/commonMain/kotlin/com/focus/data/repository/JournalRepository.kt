package com.focus.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.focus.database.FocusDatabase
import com.focus.model.JournalEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface JournalRepository {
    fun getAllJournalEntries(): Flow<List<JournalEntry>>
    suspend fun getLatestJournalEntry(): JournalEntry?
    suspend fun getJournalEntriesBetween(startDate: Long, endDate: Long): List<JournalEntry>
    suspend fun insertJournalEntry(entry: JournalEntry)
    suspend fun deleteJournalEntry(id: String)
    suspend fun deleteAllJournalEntries()
    suspend fun getAllJournalEntriesDirect(): List<JournalEntry>
    suspend fun insertJournalEntriesDirect(entries: List<JournalEntry>)
}

class SqlDelightJournalRepository(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : JournalRepository {
    private val queries = database.focusDatabaseQueries

    override fun getAllJournalEntries(): Flow<List<JournalEntry>> {
        return queries.selectAllJournalEntries()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map { JournalEntry(it.id, it.content, it.timestamp, it.dateDisplay, it.updatedAt) }
            }
    }

    override suspend fun getLatestJournalEntry(): JournalEntry? = withContext(ioDispatcher) {
        queries.getLatestJournalEntry()
            .executeAsOneOrNull()
            ?.let { JournalEntry(it.id, it.content, it.timestamp, it.dateDisplay, it.updatedAt) }
    }

    override suspend fun getJournalEntriesBetween(startDate: Long, endDate: Long): List<JournalEntry> = withContext(ioDispatcher) {
        queries.getJournalEntriesBetween(startDate, endDate)
            .executeAsList()
            .map { JournalEntry(it.id, it.content, it.timestamp, it.dateDisplay, it.updatedAt) }
    }

    override suspend fun insertJournalEntry(entry: JournalEntry) = withContext(ioDispatcher) {
        queries.insertJournalEntry(entry.id, entry.content, entry.timestamp, entry.dateDisplay, entry.updatedAt)
    }

    override suspend fun deleteJournalEntry(id: String) = withContext(ioDispatcher) {
        queries.deleteJournalEntry(id)
    }

    override suspend fun deleteAllJournalEntries() = withContext(ioDispatcher) {
        queries.deleteAllJournalEntries()
    }

    override suspend fun getAllJournalEntriesDirect(): List<JournalEntry> = withContext(ioDispatcher) {
        queries.selectAllJournalEntries()
            .executeAsList()
            .map { JournalEntry(it.id, it.content, it.timestamp, it.dateDisplay, it.updatedAt) }
    }

    override suspend fun insertJournalEntriesDirect(entries: List<JournalEntry>) = withContext(ioDispatcher) {
        database.transaction {
            entries.forEach {
                queries.insertJournalEntry(it.id, it.content, it.timestamp, it.dateDisplay, it.updatedAt)
            }
        }
    }
}
