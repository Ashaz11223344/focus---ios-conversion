package com.focus.domain

/**
 * Pure domain logic for streak tracking, grace periods, and milestone evaluation.
 */
object StreakCalculator {

    private const val MILLIS_IN_DAY = 86_400_000L

    data class StreakState(
        val streakCount: Int,
        val lastCompletionDate: Long,
        val graceDaysUsedThisWeek: Int,
        val isCompletedToday: Boolean
    )

    /**
     * Determines whether two epoch timestamps fall on the exact same calendar day (UTC-based).
     */
    fun isSameDay(timeA: Long, timeB: Long): Boolean {
        return (timeA / MILLIS_IN_DAY) == (timeB / MILLIS_IN_DAY)
    }

    /**
     * Determines whether timeA was the calendar day immediately preceding timeB.
     */
    fun isConsecutiveDay(timePrevious: Long, timeCurrent: Long): Boolean {
        val dayPrev = timePrevious / MILLIS_IN_DAY
        val dayCurr = timeCurrent / MILLIS_IN_DAY
        return dayCurr - dayPrev == 1L
    }

    /**
     * Calculates the updated streak state when user completes today's intent.
     */
    fun recordCompletion(
        currentTime: Long,
        currentState: StreakState,
        maxGraceDaysPerWeek: Int = 1
    ): StreakState {
        if (currentState.lastCompletionDate == 0L) {
            return StreakState(
                streakCount = 1,
                lastCompletionDate = currentTime,
                graceDaysUsedThisWeek = 0,
                isCompletedToday = true
            )
        }

        if (isSameDay(currentState.lastCompletionDate, currentTime)) {
            return currentState.copy(isCompletedToday = true)
        }

        val daysDifference = (currentTime / MILLIS_IN_DAY) - (currentState.lastCompletionDate / MILLIS_IN_DAY)

        return when {
            daysDifference == 1L -> {
                // Perfect continuation
                currentState.copy(
                    streakCount = currentState.streakCount + 1,
                    lastCompletionDate = currentTime,
                    isCompletedToday = true
                )
            }
            daysDifference == 2L && currentState.graceDaysUsedThisWeek < maxGraceDaysPerWeek -> {
                // 1 missed day rescued by grace day
                currentState.copy(
                    streakCount = currentState.streakCount + 1,
                    lastCompletionDate = currentTime,
                    graceDaysUsedThisWeek = currentState.graceDaysUsedThisWeek + 1,
                    isCompletedToday = true
                )
            }
            else -> {
                // Streak broken
                StreakState(
                    streakCount = 1,
                    lastCompletionDate = currentTime,
                    graceDaysUsedThisWeek = 0,
                    isCompletedToday = true
                )
            }
        }
    }
}
