package com.example.motivation.ui.onboarding

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.isReduceMotionEnabled(): Boolean {
    val scale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
    if (scale == 0.0f) return true
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    return am.isEnabled && am.isTouchExplorationEnabled
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isReduceMotionEnabled() }
}

@Composable
fun LogoPulse(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        Box(modifier = modifier) { content() }
        return
    }
    val scale = remember { Animatable(1.0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.08f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }
    Box(
        modifier = modifier.graphicsLayer(
            scaleX = scale.value,
            scaleY = scale.value
        )
    ) {
        content()
    }
}

@Composable
fun AnimatedEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        Box(modifier = modifier) { content() }
        return
    }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }
    LaunchedEffect(Unit) {
        delay(index * 100L)
        launch {
            alpha.animateTo(1f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
        }
        launch {
            offsetY.animateTo(0f, animationSpec = tween(400, easing = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)))
        }
    }
    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha.value,
            translationY = offsetY.value
        )
    ) {
        content()
    }
}

@Composable
fun MoodIconSpring(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        Box(modifier = modifier) { content() }
        return
    }
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    Box(
        modifier = modifier.graphicsLayer(
            scaleX = scale.value,
            scaleY = scale.value
        )
    ) {
        content()
    }
}

@Composable
fun AnimatingStreakCounter(
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    color: Color,
    onFinished: () -> Unit = {}
) {
    val reduceMotion = rememberReduceMotion()
    var currentNumber by remember { mutableStateOf(1) }
    if (reduceMotion) {
        Text(text = "7", style = textStyle, color = color, modifier = modifier)
        SideEffect { onFinished() }
        return
    }
    LaunchedEffect(Unit) {
        val duration = 1200L
        val steps = 6
        val delayPerStep = duration / steps
        for (i in 2..7) {
            val progress = (i - 1).toFloat() / steps
            val easeOutDelay = (delayPerStep * (1f + progress * 1.5f)).toLong()
            delay(easeOutDelay)
            currentNumber = i
        }
        onFinished()
    }
    Text(text = "$currentNumber", style = textStyle, color = color, modifier = modifier)
}

@Composable
fun BadgeSparkle(
    modifier: Modifier = Modifier,
    trigger: Boolean
) {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion || !trigger) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width * 0.6f
        val currentRadius = maxRadius * progress.value
        val alpha = 1f - progress.value
        val dotRadius = 4.dp.toPx() * (1f - progress.value * 0.5f)
        val orangeColor = Color(0xFFFC6E20)

        for (i in 0 until 6) {
            val angle = (i * 60) * (Math.PI / 180f)
            val x = center.x + (currentRadius * kotlin.math.cos(angle)).toFloat()
            val y = center.y + (currentRadius * kotlin.math.sin(angle)).toFloat()
            drawCircle(
                color = orangeColor.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(x, y)
            )
        }
    }
}
