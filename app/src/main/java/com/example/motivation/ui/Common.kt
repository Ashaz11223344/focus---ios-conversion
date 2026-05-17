package com.example.motivation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object BoxDefaults {
    @Composable
    fun cardBorder() = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
    )
}
