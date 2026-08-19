package com.example.motivation.model

import java.util.UUID

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateDisplay: String, // Formatted date string
    val updatedAt: Long? = null
)
