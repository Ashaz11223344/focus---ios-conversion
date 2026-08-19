package com.focus

import com.focus.domain.FocusGuardRuleEngine
import com.focus.domain.QuoteRotationEngine
import com.focus.model.DndSchedule
import com.focus.model.FocusGuardRule
import com.focus.model.Quote
import com.focus.sync.ConflictResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainEnginesAndConflictResolverTest {

    @Test
    fun testFocusGuardDndActive() {
        val schedules = listOf(
            DndSchedule(1, "Work", 9, 0, 17, 0, 0b0011111, true) // Mon-Fri 9:00 - 17:00
        )

        // Monday (day 0) at 10:30 -> active
        assertTrue(FocusGuardRuleEngine.isDndActive(schedules, 10, 30, 0))

        // Monday (day 0) at 18:00 -> not active
        assertFalse(FocusGuardRuleEngine.isDndActive(schedules, 18, 0, 0))

        // Saturday (day 5) at 10:30 -> not active
        assertFalse(FocusGuardRuleEngine.isDndActive(schedules, 10, 30, 5))
    }

    @Test
    fun testFocusGuardDndCrossesMidnight() {
        val schedules = listOf(
            DndSchedule(1, "Night Sleep", 22, 0, 6, 0, 0b1111111, true) // Daily 22:00 - 06:00
        )

        assertTrue(FocusGuardRuleEngine.isDndActive(schedules, 23, 0, 0))
        assertTrue(FocusGuardRuleEngine.isDndActive(schedules, 3, 0, 0))
        assertFalse(FocusGuardRuleEngine.isDndActive(schedules, 12, 0, 0))
    }

    @Test
    fun testFocusGuardAppBlocked() {
        val rules = listOf(
            FocusGuardRule("com.instagram.android", "Instagram", 9, 0, 17, 0, 0b0011111, true)
        )

        assertTrue(FocusGuardRuleEngine.isAppBlocked(rules, "com.instagram.android", 12, 0, 0))
        assertFalse(FocusGuardRuleEngine.isAppBlocked(rules, "com.instagram.android", 20, 0, 0))
        assertFalse(FocusGuardRuleEngine.isAppBlocked(rules, "com.other.app", 12, 0, 0))
    }

    @Test
    fun testQuoteRotationDeterministic() {
        val quotes = listOf(
            Quote("Quote 1", "Motivation"),
            Quote("Quote 2", "Focus"),
            Quote("Quote 3", "Calm")
        )

        val q1 = QuoteRotationEngine.selectDailyQuote(quotes, "2026-08-19")
        val q2 = QuoteRotationEngine.selectDailyQuote(quotes, "2026-08-19")
        assertEquals(q1, q2)
    }

    @Test
    fun testConflictResolverLastWriteWins() {
        assertTrue(ConflictResolver.shouldLocalOverwriteRemote(2000L, 1000L))
        assertFalse(ConflictResolver.shouldLocalOverwriteRemote(1000L, 2000L))
        assertTrue(ConflictResolver.shouldLocalOverwriteRemote(1000L, 1000L))
    }
}
