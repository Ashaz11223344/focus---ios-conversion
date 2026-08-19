package com.focus

import com.focus.domain.StreakCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreakCalculatorTest {

    private val dayMillis = 86_400_000L

    @Test
    fun testFirstCompletionInitializesStreakToOne() {
        val now = 100 * dayMillis
        val initialState = StreakCalculator.StreakState(0, 0L, 0, false)
        val updated = StreakCalculator.recordCompletion(now, initialState)
        assertEquals(1, updated.streakCount)
        assertEquals(now, updated.lastCompletionDate)
        assertEquals(0, updated.graceDaysUsedThisWeek)
        assertTrue(updated.isCompletedToday)
    }

    @Test
    fun testConsecutiveDayIncrementsStreak() {
        val day1 = 100 * dayMillis
        val day2 = 101 * dayMillis
        val state1 = StreakCalculator.StreakState(1, day1, 0, true)
        val state2 = StreakCalculator.recordCompletion(day2, state1)
        assertEquals(2, state2.streakCount)
        assertEquals(day2, state2.lastCompletionDate)
    }

    @Test
    fun testSameDayKeepsStreakSame() {
        val day1 = 100 * dayMillis
        val day1Later = day1 + 3600000L
        val state1 = StreakCalculator.StreakState(5, day1, 0, true)
        val state2 = StreakCalculator.recordCompletion(day1Later, state1)
        assertEquals(5, state2.streakCount)
    }

    @Test
    fun testMissedOneDayUsesGraceDay() {
        val day1 = 100 * dayMillis
        val day3 = 102 * dayMillis // skipped day2
        val state1 = StreakCalculator.StreakState(5, day1, 0, true)
        val state2 = StreakCalculator.recordCompletion(day3, state1, maxGraceDaysPerWeek = 1)
        assertEquals(6, state2.streakCount)
        assertEquals(1, state2.graceDaysUsedThisWeek)
    }

    @Test
    fun testMissedTwoDaysResetsStreak() {
        val day1 = 100 * dayMillis
        val day4 = 103 * dayMillis // skipped day2, day3
        val state1 = StreakCalculator.StreakState(10, day1, 0, true)
        val state2 = StreakCalculator.recordCompletion(day4, state1, maxGraceDaysPerWeek = 1)
        assertEquals(1, state2.streakCount)
        assertEquals(0, state2.graceDaysUsedThisWeek)
    }
}
