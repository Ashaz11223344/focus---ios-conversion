package com.example.motivation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

@Composable
fun MotivationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBlack.toArgb()
            window.navigationBarColor = DarkBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}
