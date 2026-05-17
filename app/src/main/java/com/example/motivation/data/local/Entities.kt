package com.example.motivation.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val timestamp: Long,
    val dateDisplay: String
)

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moodName: String,
    val moodEmoji: String,
    val moodValue: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteQuoteEntity(
    @PrimaryKey val text: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class QuoteHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)
