package com.focus.domain

import com.focus.model.DndSchedule
import com.focus.model.FocusGuardRule

object FocusGuardRuleEngine {

    /**
     * Checks whether a specific time (hour, minute, dayOfWeek: 0 = Mon .. 6 = Sun) falls within a DND schedule.
     */
    fun isDndActive(
        schedules: List<DndSchedule>,
        currentHour: Int,
        currentMinute: Int,
        dayOfWeek: Int
    ): Boolean {
        val currentMinutes = currentHour * 60 + currentMinute
        return schedules.filter { it.isEnabled }.any { schedule ->
            val isDayActive = (schedule.daysOfWeek and (1 shl dayOfWeek)) != 0
            if (!isDayActive) return@any false

            val startMinutes = schedule.startHour * 60 + schedule.startMinute
            val endMinutes = schedule.endHour * 60 + schedule.endMinute

            if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Crosses midnight (e.g. 22:00 to 06:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        }
    }

    /**
     * Checks whether a given app package should be blocked at the current time.
     */
    fun isAppBlocked(
        rules: List<FocusGuardRule>,
        packageName: String,
        currentHour: Int,
        currentMinute: Int,
        dayOfWeek: Int
    ): Boolean {
        val rule = rules.find { it.packageName == packageName && it.isEnabled } ?: return false
        val isDayActive = (rule.daysOfWeek and (1 shl dayOfWeek)) != 0
        if (!isDayActive) return false

        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = rule.startHour * 60 + rule.startMinute
        val endMinutes = rule.endHour * 60 + rule.endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
