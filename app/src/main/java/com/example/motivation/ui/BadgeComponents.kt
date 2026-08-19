package com.example.motivation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

@Composable
fun BadgeIcon(
    icon: ImageVector,
    tier: String,
    size: Dp,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val tierColors = when (tier.lowercase()) {
        "bronze" -> listOf(Color(0xFFCD7F32), Color(0xFF8C521F), Color(0xFFCD7F32), Color(0xFFE5A97D))
        "silver" -> listOf(Color(0xFFE0E0E0), Color(0xFF7F7F7F), Color(0xFFC0C0C0), Color(0xFFE0E0E0))
        "gold" -> listOf(Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFD700), Color(0xFFFFF099))
        "platinum" -> listOf(Color(0xFFF5F5F0), Color(0xFF9E9E9E), Color(0xFFE5E4E2), Color(0xFFF5F5F0))
        "diamond" -> listOf(Color(0xFFE0F7FA), Color(0xFF5D9CEC), Color(0xFFB9F2FF), Color(0xFFE0F7FA))
        "mythic" -> listOf(Color(0xFFFF4081), Color(0xFF4A0E4E), Color(0xFF8B008B), Color(0xFFFF4081))
        else -> listOf(Color(0xFFC0C0C0), Color(0xFF7F7F7F), Color(0xFFC0C0C0))
    }

    val borderColors = when (tier.lowercase()) {
        "bronze" -> Color(0xFFCD7F32)
        "silver" -> Color(0xFFC0C0C0)
        "gold" -> Color(0xFFFFD700)
        "platinum" -> Color(0xFFE5E4E2)
        "diamond" -> Color(0xFFB9F2FF)
        "mythic" -> Color(0xFF8B008B)
        else -> Color(0xFFC0C0C0)
    }

    // Pulse animation for Mythic tier
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val scaleState = if (isUnlocked && tier.lowercase() == "mythic") {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        null
    }
    val scale = scaleState?.value ?: 1f

    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = androidx.compose.runtime.remember(size, density) { with(density) { size.toPx() } }
    val glowRadius = if (isUnlocked && tier.lowercase() == "mythic") 10.dp else 3.dp
    val shadowColor = if (isUnlocked) borderColors.copy(alpha = 0.35f) else Color.Transparent

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (isUnlocked) {
                    val glowRadiusPx = glowRadius.toPx()
                    val shadowOffsetPx = 1.5f.dp.toPx()
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            asFrameworkPaint().apply {
                                color = shadowColor.toArgb()
                                setShadowLayer(
                                    glowRadiusPx,
                                    0f,
                                    shadowOffsetPx,
                                    shadowColor.toArgb()
                                )
                            }
                        }
                        canvas.drawCircle(
                            center,
                            sizePx / 2 - 1.dp.toPx(),
                            paint
                        )
                    }
                }
            }
            .clip(CircleShape)
            .background(
                if (isUnlocked) {
                    Brush.radialGradient(
                        colors = tierColors,
                        radius = sizePx * 0.75f
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF222222), Color(0xFF161616)),
                        radius = sizePx * 0.75f
                    )
                }
            )
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = if (isUnlocked) borderColors else Color(0xFF333333),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isUnlocked) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tier.lowercase() == "mythic") Color.White else Color(0xFF121212),
                modifier = Modifier.size(size * 0.45f)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(size * 0.35f),
                tint = Color(0xFFFFE7D0).copy(alpha = 0.4f)
            )
        }
    }
}

fun getAchievementIcon(id: String): ImageVector {
    return when (id) {
        "first_step" -> Icons.Rounded.RocketLaunch
        "week_warrior" -> Icons.Rounded.Whatshot
        "month_master" -> Icons.Rounded.Star
        "year_legend" -> Icons.Rounded.EmojiEvents
        "mood_tracker" -> Icons.Rounded.Mood
        "journal_keeper" -> Icons.Rounded.Create
        "focus_ninja" -> Icons.Rounded.SelfImprovement
        "night_owl" -> Icons.Rounded.Bedtime
        "early_bird" -> Icons.Rounded.WbSunny
        "mythic_master" -> Icons.Rounded.AutoAwesome
        else -> Icons.Rounded.EmojiEvents
    }
}

@Composable
fun ProfileAvatar(
    photoUriString: String?,
    userName: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val bitmap = androidx.compose.runtime.remember(photoUriString) {
        if (!photoUriString.isNullOrBlank()) {
            try {
                android.graphics.BitmapFactory.decodeFile(photoUriString)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2E2E2E)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            val initial = if (userName.isNotBlank()) userName.take(1).uppercase() else "?"
            Text(
                text = initial,
                fontFamily = com.example.motivation.ui.theme.LiterataFontFamily,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = (size.value * 0.4).sp,
                color = Color(0xFFFFE7D0)
            )
        }
    }
}

fun getHighestTierUnlockedBadge(achievements: List<com.example.motivation.data.local.Achievement>): com.example.motivation.data.local.Achievement? {
    val tierPriority = listOf("mythic", "diamond", "platinum", "gold", "silver", "bronze")
    val unlocked = achievements.filter { it.isUnlocked }
    for (tier in tierPriority) {
        val found = unlocked.firstOrNull { it.tier.lowercase() == tier }
        if (found != null) return found
    }
    return null
}
