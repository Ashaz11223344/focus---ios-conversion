package com.focus

import com.focus.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun testMoodEntrySerialization() {
        val mood = MoodEntry(
            id = 1,
            moodName = "Happy",
            moodEmoji = "😊",
            moodValue = 5,
            timestamp = 1700000000000L
        )
        val encoded = json.encodeToString(mood)
        val decoded = json.decodeFromString<MoodEntry>(encoded)
        assertEquals(mood, decoded)
    }

    @Test
    fun testJournalEntrySerialization() {
        val journal = JournalEntry(
            id = "test-uuid-123",
            content = "Productive morning session!",
            timestamp = 1700000000000L,
            dateDisplay = "Today",
            updatedAt = 1700000500000L
        )
        val encoded = json.encodeToString(journal)
        val decoded = json.decodeFromString<JournalEntry>(encoded)
        assertEquals(journal, decoded)
    }

    @Test
    fun testBackupPayloadSerialization() {
        val payload = BackupPayload(
            version = 1,
            timestamp = 1700000000000L,
            journalEntries = listOf(
                JournalEntry("id1", "Entry 1", 1700000000000L, "Today")
            ),
            moodEntries = listOf(
                MoodEntry(1, "Focused", "🎯", 4, 1700000000000L)
            ),
            favorites = listOf(
                Quote("Focus is power", "Motivation")
            ),
            dndSchedules = listOf(
                DndSchedule(1, "Deep Work", 9, 0, 12, 0, 31, true)
            ),
            appBlockRules = listOf(
                FocusGuardRule("com.distracting.app", "Distraction", 9, 0, 17, 0, 31, true)
            ),
            achievements = listOf(
                Achievement("first_step", "First Step", "Desc", "🚀", "bronze", "#CD7F32", null, false, 0, 1, "sessions")
            ),
            settings = SettingsBackup(
                userName = "Ashaz",
                streakCount = 7,
                themeMode = "DARK"
            )
        )
        val encoded = json.encodeToString(payload)
        val decoded = json.decodeFromString<BackupPayload>(encoded)
        assertEquals(payload.version, decoded.version)
        assertEquals(payload.settings.userName, decoded.settings.userName)
        assertEquals(1, decoded.journalEntries.size)
        assertEquals(1, decoded.moodEntries.size)
        assertEquals(1, decoded.dndSchedules.size)
    }
}
