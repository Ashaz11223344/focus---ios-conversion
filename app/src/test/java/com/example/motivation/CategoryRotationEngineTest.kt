package com.example.motivation

import com.example.motivation.receiver.CategoryRotationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryRotationEngineTest {

    @Test
    fun getTodayCategories_emptySelection_returnsEmpty() {
        val today = CategoryRotationEngine.getTodayCategories(
            selectedCategories = emptyList(),
            pointer = 0,
            slotsToday = 2
        )
        assertEquals(emptyList<String>(), today)
    }

    @Test
    fun getTodayCategories_normalRotation_wrapsCorrectly() {
        val categories = listOf("motivation", "love", "focus")
        
        // Day 1: Pointer = 0, Slots = 2
        val day1 = CategoryRotationEngine.getTodayCategories(categories, 0, 2)
        assertEquals(listOf("motivation", "love"), day1)

        // Day 2: Pointer = 2, Slots = 2
        val day2 = CategoryRotationEngine.getTodayCategories(categories, 2, 2)
        assertEquals(listOf("focus", "motivation"), day2)

        // Day 3: Pointer = 1, Slots = 2
        val day3 = CategoryRotationEngine.getTodayCategories(categories, 1, 2)
        assertEquals(listOf("love", "focus"), day3)
    }

    @Test
    fun getTodayCategories_fewerCategoriesThanSlots_limitsToSize() {
        val categories = listOf("motivation")
        val today = CategoryRotationEngine.getTodayCategories(categories, 0, 3)
        assertEquals(listOf("motivation"), today)
    }

    @Test
    fun advancePointer_wrapsCorrectly() {
        val categories = listOf("motivation", "love", "focus")
        
        val p1 = CategoryRotationEngine.advancePointer(0, categories, 2)
        assertEquals(2, p1)

        val p2 = CategoryRotationEngine.advancePointer(2, categories, 2)
        assertEquals(1, p2)

        val p3 = CategoryRotationEngine.advancePointer(1, categories, 2)
        assertEquals(0, p3)
    }

    @Test
    fun advancePointer_emptySelection_returnsZero() {
        val p = CategoryRotationEngine.advancePointer(5, emptyList(), 2)
        assertEquals(0, p)
    }

    @Test
    fun fullCycleDays_calculatesCorrectly() {
        // LCM(3, 2) = 6. Cycle length = 6 / 2 = 3 days
        assertEquals(3, CategoryRotationEngine.fullCycleDays(3, 2))

        // LCM(5, 3) = 15. Cycle length = 15 / 3 = 5 days
        assertEquals(5, CategoryRotationEngine.fullCycleDays(5, 3))

        // LCM(4, 2) = 4. Cycle length = 4 / 2 = 2 days
        assertEquals(2, CategoryRotationEngine.fullCycleDays(4, 2))

        // LCM(3, 3) = 3. Cycle length = 3 / 3 = 1 day
        assertEquals(1, CategoryRotationEngine.fullCycleDays(3, 3))

        // Corner cases
        assertEquals(0, CategoryRotationEngine.fullCycleDays(0, 3))
        assertEquals(0, CategoryRotationEngine.fullCycleDays(3, 0))
    }
}
