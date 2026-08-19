package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class MoodEntry(
    val id: Long = 0,
    val moodName: String,
    val moodEmoji: String,
    val moodValue: Int,
    val timestamp: Long
)
