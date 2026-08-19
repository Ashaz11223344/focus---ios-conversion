package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class PrivateJournalEntry(
    val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val wordCount: Int
)
