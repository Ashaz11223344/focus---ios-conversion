package com.focus.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Interface for remote network synchronization endpoint.
 */
interface SyncNetworkClient {
    suspend fun uploadItem(item: SyncItem): Boolean
}

class SyncOrchestrator(
    private val syncQueue: SyncQueue,
    private val networkClient: SyncNetworkClient? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun synchronize(): SyncResult = withContext(ioDispatcher) {
        val client = networkClient ?: return@withContext SyncResult(0, 0, false)
        val pendingItems = syncQueue.getPendingItems()
        var successCount = 0
        var failCount = 0

        for (item in pendingItems) {
            syncQueue.markSyncing(item.id)
            val success = try {
                client.uploadItem(item)
            } catch (e: Exception) {
                false
            }

            if (success) {
                syncQueue.markCompleted(item.id)
                successCount++
            } else {
                syncQueue.markFailed(item.id, item.retryCount + 1)
                failCount++
            }
        }

        SyncResult(
            syncedCount = successCount,
            failedCount = failCount,
            isFullySynced = failCount == 0
        )
    }
}

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val isFullySynced: Boolean
)
