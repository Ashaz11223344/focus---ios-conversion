package com.focus.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncOperation {
    INSERT, UPDATE, DELETE
}

@Serializable
enum class SyncStatus {
    PENDING, SYNCING, FAILED, COMPLETED
}

@Serializable
data class SyncItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    val timestamp: Long,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0
)
