package com.focus.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.focus.database.FocusDatabase
import com.focus.model.PrivateJournalEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface PrivateJournalRepository {
    fun getAllPrivateEntries(): Flow<List<PrivateJournalEntry>>
    suspend fun getPrivateEntriesBetween(startDate: Long, endDate: Long): List<PrivateJournalEntry>
    suspend fun insertPrivateEntry(content: String, timestamp: Long, wordCount: Int, id: Long = 0)
    suspend fun deletePrivateEntry(id: Long)
    suspend fun deleteAllPrivateEntries()
    suspend fun getAllPrivateEntriesDirect(): List<PrivateJournalEntry>
    suspend fun insertPrivateEntriesDirect(entries: List<PrivateJournalEntry>)
}

class SqlDelightPrivateJournalRepository(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PrivateJournalRepository {
    private val queries = database.focusDatabaseQueries

    override fun getAllPrivateEntries(): Flow<List<PrivateJournalEntry>> {
        return queries.selectAllPrivateEntries()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list ->
                list.map { PrivateJournalEntry(it.id, it.content, it.timestamp, it.wordCount.toInt()) }
            }
    }

    override suspend fun getPrivateEntriesBetween(startDate: Long, endDate: Long): List<PrivateJournalEntry> = withContext(ioDispatcher) {
        queries.getPrivateEntriesBetween(startDate, endDate)
            .executeAsList()
            .map { PrivateJournalEntry(it.id, it.content, it.timestamp, it.wordCount.toInt()) }
    }

    override suspend fun insertPrivateEntry(
        content: String,
        timestamp: Long,
        wordCount: Int,
        id: Long
    ) = withContext(ioDispatcher) {
        val entryId = if (id == 0L) null else id
        queries.insertPrivateEntry(entryId, content, timestamp, wordCount.toLong())
    }

    override suspend fun deletePrivateEntry(id: Long) = withContext(ioDispatcher) {
        queries.deletePrivateEntry(id)
    }

    override suspend fun deleteAllPrivateEntries() = withContext(ioDispatcher) {
        queries.deleteAllPrivateEntries()
    }

    override suspend fun getAllPrivateEntriesDirect(): List<PrivateJournalEntry> = withContext(ioDispatcher) {
        queries.selectAllPrivateEntries()
            .executeAsList()
            .map { PrivateJournalEntry(it.id, it.content, it.timestamp, it.wordCount.toInt()) }
    }

    override suspend fun insertPrivateEntriesDirect(entries: List<PrivateJournalEntry>) = withContext(ioDispatcher) {
        database.transaction {
            entries.forEach {
                val entryId = if (it.id == 0L) null else it.id
                queries.insertPrivateEntry(entryId, it.content, it.timestamp, it.wordCount.toLong())
            }
        }
    }
}
