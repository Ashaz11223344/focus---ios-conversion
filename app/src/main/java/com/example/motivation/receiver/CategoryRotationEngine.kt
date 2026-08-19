package com.example.motivation.receiver

import kotlin.math.min

object CategoryRotationEngine {

    /**
     * Given the full ordered list of selected categories, the current
     * pointer position, and how many notification slots exist today,
     * returns the ordered list of categories to use for today's notifications.
     */
    fun getTodayCategories(
        selectedCategories: List<String>,
        pointer: Int,
        slotsToday: Int
    ): List<String> {
        if (selectedCategories.isEmpty()) return emptyList()
        val count = min(slotsToday, selectedCategories.size)
        return List(count) { i ->
            selectedCategories[(pointer + i) % selectedCategories.size]
        }
    }

    /**
     * Advances the pointer by slotsToday positions, wrapping via modulo.
     * Call this AFTER scheduling today's notifications, not before.
     */
    fun advancePointer(
        currentPointer: Int,
        selectedCategories: List<String>,
        slotsToday: Int
    ): Int {
        if (selectedCategories.isEmpty()) return 0
        return (currentPointer + slotsToday) % selectedCategories.size
    }

    /**
     * Returns how many days until the full cycle repeats.
     * Used for displaying "full cycle" info to the user.
     * Cycle length in days = LCM(N, M) / M
     */
    fun fullCycleDays(totalCategories: Int, slotsPerDay: Int): Int {
        if (totalCategories == 0 || slotsPerDay == 0) return 0
        val lcm = lcm(totalCategories, slotsPerDay)
        return lcm / slotsPerDay
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    private fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
}
