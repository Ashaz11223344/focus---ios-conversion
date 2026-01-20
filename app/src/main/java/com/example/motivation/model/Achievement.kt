package com.example.motivation.model

import androidx.annotation.DrawableRes
import com.example.motivation.R

/**
 * Represents a single achievement milestone.
 *
 * @property id A unique identifier for the achievement.
 * @property title The name of the achievement.
 * @property description A short description of what the achievement represents.
 * @property streakRequired The streak count needed to unlock this achievement.
 * @property iconResId The drawable resource ID for the achievement's icon.
 * @property isUnlocked Whether the user has unlocked this achievement.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val streakRequired: Int,
    @DrawableRes val iconResId: Int,
    var isUnlocked: Boolean = false
)

/**
 * Static list of all available achievements in the app.
 */
object AchievementsList {
    val allAchievements = listOf(
        Achievement(
            id = "one_day",
            title = "First Intent Completed",
            description = "You completed your first daily intent.",
            streakRequired = 1,
            iconResId = R.drawable.ic_achievement_1 // Placeholder icon
        ),
        Achievement(
            id = "three_days",
            title = "Consistency Beginner",
            description = "You've shown up 3 days in a row.",
            streakRequired = 3,
            iconResId = R.drawable.ic_achievement_3 // Placeholder icon
        ),
        Achievement(
            id = "seven_days",
            title = "Discipline Builder",
            description = "A full week of building discipline.",
            streakRequired = 7,
            iconResId = R.drawable.ic_achievement_7 // Placeholder icon
        ),
        Achievement(
            id = "fourteen_days",
            title = "Habit Former",
            description = "Two weeks of consistent effort.",
            streakRequired = 14,
            iconResId = R.drawable.ic_achievement_14 // Placeholder icon
        ),
        Achievement(
            id = "thirty_days",
            title = "Unbreakable",
            description = "One month of unwavering focus.",
            streakRequired = 30,
            iconResId = R.drawable.ic_achievement_30 // Placeholder icon
        ),
        Achievement(
            id = "sixty_days",
            title = "Momentum Master",
            description = "60 days of focus. Discipline is now your rhythm.",
            streakRequired = 60,
            iconResId = R.drawable.ic_achievement_60 // Placeholder icon
        ),
        Achievement(
            id = "90_days",
            title = "Iron Will",
            description = "90 days in. Motivation fades, discipline remains.",
            streakRequired = 90,
            iconResId = R.drawable.ic_achievement_90 // Placeholder icon
        ),
        Achievement(
            id = "120_days",
            title = "Habit Architect",
            description = "You’ve designed a life powered by consistency.",
            streakRequired = 120,
            iconResId = R.drawable.ic_achievement_120 // Placeholder icon
        ),
        Achievement(
            id = "160_days",
            title = "Relentless",
            description = "160 days of showing up, no matter what.",
            streakRequired = 160,
            iconResId = R.drawable.ic_achievement_160 // Placeholder icon
        ),
        Achievement(
            id = "190_days",
            title = "Self-Control Elite",
            description = "You choose progress over comfort every single day.",
            streakRequired = 190,
            iconResId = R.drawable.ic_achievement_190 // Placeholder icon
        ),
        Achievement(
            id = "220_days",
            title = "Focused Mind",
            description = "Your focus is sharper than ever.",
            streakRequired = 220,
            iconResId = R.drawable.ic_achievement_220 // Placeholder icon
        ),
        Achievement(
            id = "250_days",
            title = "Discipline Veteran",
            description = "You’ve survived the long grind and stayed true.",
            streakRequired = 250,
            iconResId = R.drawable.ic_achievement_250 // Placeholder icon
        ),
        Achievement(
            id = "280_days",
            title = "Unshakable",
            description = "Distractions no longer control you.",
            streakRequired = 280,
            iconResId = R.drawable.ic_achievement_280 // Placeholder icon
        ),
        Achievement(
            id = "310_days",
            title = "Mental Fortress",
            description = "Your mind is built on discipline and resolve.",
            streakRequired = 310,
            iconResId = R.drawable.ic_achievement_310 // Placeholder icon
        ),
        Achievement(
            id = "330_days",
            title = "Master of Consistency",
            description = "Consistency is no longer effort. It’s identity.",
            streakRequired = 330,
            iconResId = R.drawable.ic_achievement_330 // Placeholder icon
        ),
        Achievement(
            id = "365_days",
            title = "One Year Unbroken",
            description = "365 days. You kept the promise to yourself.",
            streakRequired = 365,
            iconResId = R.drawable.ic_achievement_365 // Placeholder icon
        ),
    )
}
