package com.focus.sync

import com.focus.database.FocusDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

interface SyncQueue {
    suspend fun enqueue(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        payload: String
    )
    suspend fun getPendingItems(): List<SyncItem>
    suspend fun markSyncing(id: String)
    suspend fun markCompleted(id: String)
    suspend fun markFailed(id: String, retryCount: Int)
    suspend fun clear()
}

class SqlDelightSyncQueue(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SyncQueue {
    private val queries = database.focusDatabaseQueries

    override suspend fun enqueue(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        payload: String
    ) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val id = "${entityType}_${entityId}_$now"
        queries.insertSyncItem(
            id = id,
            entityType = entityType,
            entityId = entityId,
            operation = operation.name,
            payload = payload,
            timestamp = now,
            status = SyncStatus.PENDING.name,
            retryCount = 0L
        )
    }

    override suspend fun getPendingItems(): List<SyncItem> = withContext(ioDispatcher) {
        queries.selectAllPendingSyncItems().executeAsList().map {
            SyncItem(
                id = it.id,
                entityType = it.entityType,
                entityId = it.entityId,
                operation = SyncOperation.valueOf(it.operation),
                payload = it.payload,
                timestamp = it.timestamp,
                status = SyncStatus.valueOf(it.status),
                retryCount = it.retryCount.toInt()
            )
        }
    }

    override suspend fun markSyncing(id: String) = withContext(ioDispatcher) {
        queries.updateSyncItemStatus(status = SyncStatus.SYNCING.name, retryCount = 0L, id = id)
    }

    override suspend fun markCompleted(id: String) = withContext(ioDispatcher) {
        queries.deleteSyncItem(id)
    }

    override suspend fun markFailed(id: String, retryCount: Int) = withContext(ioDispatcher) {
        queries.updateSyncItemStatus(status = SyncStatus.FAILED.name, retryCount = retryCount.toLong(), id = id)
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        queries.clearSyncQueue()
    }
}
