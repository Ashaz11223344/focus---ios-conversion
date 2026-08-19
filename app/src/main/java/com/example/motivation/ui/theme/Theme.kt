package com.example.motivation.ui.theme

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.core.view.drawToBitmap

private val ColorScheme = darkColorScheme(
    primary = VibrantOrange,
    secondary = CreamBeige,
    tertiary = CharcoalGray,
    background = DarkBlack,
    surface = CharcoalGray,
    onPrimary = DarkBlack,
    onSecondary = DarkBlack,
    onTertiary = CreamBeige,
    onBackground = CreamBeige,
    onSurface = CreamBeige,
    surfaceVariant = CharcoalGray.copy(alpha = 0.7f),
    onSurfaceVariant = CreamBeige.copy(alpha = 0.8f),
    outline = GraySecondary
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantOrange,
    secondary = TextPrimaryLight,
    tertiary = SurfaceContainerLight,
    background = LightBase,
    surface = SurfaceElevated,
    onPrimary = OnOrangeText,
    onSecondary = LightBase,
    onTertiary = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = TextPrimaryLight.copy(alpha = 0.8f),
    outline = TextSecondaryLight
)

@Composable
fun MotivationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    // 1. Resolve system theme fallback
    val isSystemDark = isSystemInDarkTheme()
    val resolvedDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var appliedDarkTheme by remember { mutableStateOf(resolvedDarkTheme) }
    var snapshotBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val animatable = remember { Animatable(0f) }
    val view = LocalView.current

    // 2. Trigger Circular Reveal when theme changes
    LaunchedEffect(resolvedDarkTheme) {
        if (resolvedDarkTheme != appliedDarkTheme) {
            try {
                // Take a screenshot of the current UI layout before color change
                val bitmap = view.drawToBitmap().asImageBitmap()
                snapshotBitmap = bitmap
            } catch (e: Exception) {
                snapshotBitmap = null
            }
            
            // Switch colors to trigger recomposition of live elements
            appliedDarkTheme = resolvedDarkTheme
            
            // Animate progress multiplier from 0.0f to 1.0f
            animatable.snapTo(0f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EaseInOutCubic)
            )
            
            // Remove snapshot overlay when animation finishes
            snapshotBitmap = null
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val backgroundArgb = if (appliedDarkTheme) DarkBlack.toArgb() else LightBase.toArgb()
            
            // Update bar background fill colors
            window.statusBarColor = backgroundArgb
            window.navigationBarColor = backgroundArgb
            
            // Invert system icon color schemes dynamically
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !appliedDarkTheme
            controller.isAppearanceLightNavigationBars = !appliedDarkTheme
        }
    }

    val colorScheme = if (appliedDarkTheme) ColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live, newly-themed view hierarchy
            content()

            // 3. Render old snapshot on top, reveal live contents in expanding circle
            snapshotBitmap?.let { bitmap ->
                val progress = animatable.value
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = kotlin.math.hypot(size.width, size.height)
                    val currentRadius = progress * maxRadius
                    
                    val path = Path().apply {
                        addOval(Rect(center, currentRadius))
                    }
                    
                    // Difference clip: keeps everything EXCEPT the expanding circle area
                    clipPath(path, clipOp = ClipOp.Difference) {
                        drawImage(
                            image = bitmap,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    }
                }
            }
        }
    }
}
