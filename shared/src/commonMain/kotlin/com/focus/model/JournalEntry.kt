package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class JournalEntry(
    val id: String,
    val content: String,
    val timestamp: Long,
    val dateDisplay: String,
    val updatedAt: Long? = null
)
