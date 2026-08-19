package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusGuardRule(
    val packageName: String,
    val appName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Int,
    val isEnabled: Boolean = true
)

@Serializable
data class DndSchedule(
    val id: Long = 0,
    val label: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Int,
    val isEnabled: Boolean = true
)
