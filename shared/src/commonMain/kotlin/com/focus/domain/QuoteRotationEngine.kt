package com.focus.domain

import com.focus.model.Quote

object QuoteRotationEngine {

    /**
     * Selects a deterministic daily quote from a list based on the day of the year / date string.
     */
    fun selectDailyQuote(quotes: List<Quote>, dateSeed: String): Quote? {
        if (quotes.isEmpty()) return null
        val hash = dateSeed.hashCode().toLong() and 0x7FFFFFFF
        val index = (hash % quotes.size).toInt()
        return quotes[index]
    }

    /**
     * Cycles through selected categories to rotate quote delivery.
     */
    fun getNextCategoryInRotation(
        categories: List<String>,
        currentPointer: Int
    ): Pair<String, Int> {
        if (categories.isEmpty()) return Pair("Motivation", 0)
        val safeIndex = currentPointer % categories.size
        val nextPointer = (safeIndex + 1) % categories.size
        return Pair(categories[safeIndex], nextPointer)
    }
}
