package com.example.motivation.model

data class MoodEntry(
    val id: Int,
    val moodName: String,
    val moodEmoji: String,
    val moodValue: Int,
    val timestamp: Long
)
