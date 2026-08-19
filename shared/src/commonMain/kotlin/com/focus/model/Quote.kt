package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class Quote(
    val text: String,
    val category: String
)
