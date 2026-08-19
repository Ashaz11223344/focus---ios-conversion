package com.focus.model

import kotlinx.serialization.Serializable

@Serializable
data class QuoteCategory(
    val id: String,
    val displayName: String,
    val iconName: String
) {
    companion object {
        val Motivation = QuoteCategory("motivation", "Motivation", "bolt")
        val Love = QuoteCategory("love", "Love", "favorite")
        val Focus = QuoteCategory("focus", "Focus", "center_focus_strong")
        val Calm = QuoteCategory("calm", "Calm", "self_improvement")
        val HardWork = QuoteCategory("hard_work", "Hard Work", "fitness_center")
        val Success = QuoteCategory("success", "Success", "emoji_events")
        val Life = QuoteCategory("life", "Life", "park")
        val Wisdom = QuoteCategory("wisdom", "Wisdom", "menu_book")
        val Courage = QuoteCategory("courage", "Courage", "shield")
        val Growth = QuoteCategory("growth", "Growth", "trending_up")
        val Healing = QuoteCategory("healing", "Healing", "healing")
        val Happiness = QuoteCategory("happiness", "Happiness", "sentiment_satisfied")
        val Hope = QuoteCategory("hope", "Hope", "light_mode")
        val Inspiration = QuoteCategory("inspiration", "Inspiration", "tips_and_updates")
        val Friendship = QuoteCategory("friendship", "Friendship", "people")
        val Faith = QuoteCategory("faith", "Faith", "volunteer_activism")

        val all: List<QuoteCategory> = listOf(
            Motivation, Love, Focus, Calm, HardWork, Success, Life, Wisdom,
            Courage, Growth, Healing, Happiness, Hope, Inspiration, Friendship, Faith
        )

        fun fromId(id: String): QuoteCategory? {
            return all.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
