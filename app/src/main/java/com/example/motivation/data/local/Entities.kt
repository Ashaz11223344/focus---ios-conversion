package com.example.motivation.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val timestamp: Long,
    val dateDisplay: String,
    val updatedAt: Long? = null
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

@Entity(tableName = "private_journal_entries")
data class PrivateJournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long,
    val wordCount: Int
)

@Entity(tableName = "dnd_schedules")
data class DndScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,                  // e.g. "Morning Focus"
    val startHour: Int,                 // 0–23
    val startMinute: Int,               // 0–59
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Int,                // Bitmask: bit 0 = Mon, bit 6 = Sun
    val isEnabled: Boolean = true
)

@Entity(tableName = "app_block_rules")
data class AppBlockRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Int,                // Same bitmask as above
    val isEnabled: Boolean = true
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val achievementId: String,      // "7_day_streak", "mood_tracker", etc.
    val title: String,              // "7-Day Streak"
    val description: String,
    val iconEmoji: String,          // 🔥
    val tier: String,               // "gold", "silver", "mythic"
    val tierColor: String,          // "#FFD700"
    val unlockedDate: Long?,        // null if locked
    val isUnlocked: Boolean,
    val progressCurrent: Int,       // Current progress (e.g., 5/7 days)
    val progressTarget: Int,        // Target (e.g., 7)
    val category: String            // "streak", "mood", "journal", "sessions"
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)


