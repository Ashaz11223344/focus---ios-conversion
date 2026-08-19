package com.focus.domain

import com.focus.model.MoodEntry

data class MoodAnalyticsReport(
    val totalLogs: Int,
    val averageMoodValue: Double,
    val dominantEmoji: String?,
    val moodDistribution: Map<Int, Int>,
    val trend: MoodTrend
)

enum class MoodTrend {
    IMPROVING,
    STEADY,
    DECLINING,
    INSUFFICIENT_DATA
}

object MoodAnalyticsEngine {

    fun analyze(entries: List<MoodEntry>): MoodAnalyticsReport {
        if (entries.isEmpty()) {
            return MoodAnalyticsReport(
                totalLogs = 0,
                averageMoodValue = 0.0,
                dominantEmoji = null,
                moodDistribution = emptyMap(),
                trend = MoodTrend.INSUFFICIENT_DATA
            )
        }

        val total = entries.size
        val avg = entries.map { it.moodValue }.average()

        val dominantEmoji = entries.groupingBy { it.moodEmoji }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val distribution = entries.groupingBy { it.moodValue }
            .eachCount()

        val trend = calculateTrend(entries)

        return MoodAnalyticsReport(
            totalLogs = total,
            averageMoodValue = avg,
            dominantEmoji = dominantEmoji,
            moodDistribution = distribution,
            trend = trend
        )
    }

    private fun calculateTrend(entries: List<MoodEntry>): MoodTrend {
        if (entries.size < 4) return MoodTrend.INSUFFICIENT_DATA

        val sorted = entries.sortedBy { it.timestamp }
        val half = sorted.size / 2
        val firstHalfAvg = sorted.take(half).map { it.moodValue }.average()
        val secondHalfAvg = sorted.takeLast(half).map { it.moodValue }.average()

        val diff = secondHalfAvg - firstHalfAvg
        return when {
            diff > 0.3 -> MoodTrend.IMPROVING
            diff < -0.3 -> MoodTrend.DECLINING
            else -> MoodTrend.STEADY
        }
    }
}
