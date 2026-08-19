package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class Achievement(
    val achievementId: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val tier: String,
    val tierColor: String,
    val unlockedDate: Long? = null,
    val isUnlocked: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1,
    val category: String
)
