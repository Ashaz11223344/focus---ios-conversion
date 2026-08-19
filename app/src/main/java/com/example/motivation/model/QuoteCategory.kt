package com.example.motivation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class QuoteCategory(
    val id: String,
    val displayName: String,
    val icon: ImageVector
) {
    object Motivation : QuoteCategory("motivation", "Motivation", Icons.Default.Bolt)
    object Love : QuoteCategory("love", "Love", Icons.Default.Favorite)
    object Focus : QuoteCategory("focus", "Focus", Icons.Default.CenterFocusStrong)
    object Calm : QuoteCategory("calm", "Calm", Icons.Default.SelfImprovement)
    object HardWork : QuoteCategory("hard_work", "Hard Work", Icons.Default.FitnessCenter)
    object Success : QuoteCategory("success", "Success", Icons.Default.EmojiEvents)
    object Life : QuoteCategory("life", "Life", Icons.Default.Park)
    object Wisdom : QuoteCategory("wisdom", "Wisdom", Icons.Default.MenuBook)

    // Extra categories present in quotes.json
    object Courage : QuoteCategory("courage", "Courage", Icons.Default.Shield)
    object Growth : QuoteCategory("growth", "Growth", Icons.Default.TrendingUp)
    object Healing : QuoteCategory("healing", "Healing", Icons.Default.Healing)
    object Happiness : QuoteCategory("happiness", "Happiness", Icons.Default.SentimentSatisfied)
    object Hope : QuoteCategory("hope", "Hope", Icons.Default.LightMode)
    object Inspiration : QuoteCategory("inspiration", "Inspiration", Icons.Default.TipsAndUpdates)
    object Friendship : QuoteCategory("friendship", "Friendship", Icons.Default.People)
    object Faith : QuoteCategory("faith", "Faith", Icons.Default.VolunteerActivism)

    companion object {
        val all: List<QuoteCategory> = listOf(
            Motivation, Love, Focus, Calm, HardWork, Success, Life, Wisdom,
            Courage, Growth, Healing, Happiness, Hope, Inspiration, Friendship, Faith
        )

        fun fromId(id: String): QuoteCategory? {
            return all.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
