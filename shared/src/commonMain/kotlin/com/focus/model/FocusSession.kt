package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusSession(
    val id: Long = 0,
    val timestamp: Long
)
