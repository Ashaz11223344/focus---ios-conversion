package com.focus

import com.focus.domain.MoodAnalyticsEngine
import com.focus.domain.MoodTrend
import com.focus.model.MoodEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class MoodAnalyticsEngineTest {

    @Test
    fun testEmptyEntriesReturnsEmptyReport() {
        val report = MoodAnalyticsEngine.analyze(emptyList())
        assertEquals(0, report.totalLogs)
        assertEquals(0.0, report.averageMoodValue)
        assertEquals(MoodTrend.INSUFFICIENT_DATA, report.trend)
    }

    @Test
    fun testMoodCalculationAndDominantEmoji() {
        val entries = listOf(
            MoodEntry(1, "Happy", "😊", 5, 1000L),
            MoodEntry(2, "Happy", "😊", 5, 2000L),
            MoodEntry(3, "Calm", "😌", 4, 3000L),
            MoodEntry(4, "Productive", "🎯", 4, 4000L)
        )
        val report = MoodAnalyticsEngine.analyze(entries)
        assertEquals(4, report.totalLogs)
        assertEquals(4.5, report.averageMoodValue)
        assertEquals("😊", report.dominantEmoji)
        assertEquals(2, report.moodDistribution[5])
        assertEquals(2, report.moodDistribution[4])
    }

    @Test
    fun testImprovingMoodTrend() {
        val entries = listOf(
            MoodEntry(1, "Sad", "😢", 2, 1000L),
            MoodEntry(2, "Sad", "😢", 2, 2000L),
            MoodEntry(3, "Happy", "😊", 5, 3000L),
            MoodEntry(4, "Happy", "😊", 5, 4000L)
        )
        val report = MoodAnalyticsEngine.analyze(entries)
        assertEquals(MoodTrend.IMPROVING, report.trend)
    }
}
