package com.example.motivation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DriftingWord(
    val id: Int,
    val text: String,
    val xDp: Float,
    val yDp: Float,
    val fontSizeSp: Float,
    val driftYDp: Float,
    val durationMs: Int,
    val maxOpacity: Float
)

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install system splash screen API for entry point compatibility
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Seamless transparent edge-to-edge system bars configuration
        enableEdgeToEdge()

        setContent {
            val settingsDataStore = remember { com.example.motivation.data.SettingsDataStore(applicationContext) }
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = com.example.motivation.ui.theme.ThemeMode.DARK)

            com.example.motivation.ui.theme.MotivationTheme(themeMode = themeMode) {
                SplashAnimatedScreen(onFinished = {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    
                    // Crossfade finish transition supporting modern Android APIs
                    if (Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(
                            Activity.OVERRIDE_TRANSITION_CLOSE,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                })
            }
        }
    }
}

@Composable
fun SplashAnimatedScreen(onFinished: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Timeline Animation States
    val animLogoAlpha = remember { Animatable(0f) }
    val animLogoScale = remember { Animatable(0.85f) }
    val animLogoSlide = remember { Animatable(15f) }

    val animAppAlpha = remember { Animatable(0f) }
    val animAppSlide = remember { Animatable(10f) }

    val animTaglineAlpha = remember { Animatable(0f) }
    val animTaglineSlide = remember { Animatable(10f) }

    val animTrackAlpha = remember { Animatable(0f) }
    val animProgress = remember { Animatable(0f) }

    val animScreenAlpha = remember { Animatable(1f) }

    // Floating Words Lifecycle State
    val activeWords = remember { mutableStateListOf<DriftingWord>() }

    LaunchedEffect(Unit) {
        var wordIdCounter = 0
        val wordsList = listOf(
            "Start.", "Breathe.", "Keep going.", "Trust.", "Focus.",
            "Reflect.", "Create.", "Rise.", "Stay.", "Think.",
            "Slow down.", "One step."
        )

        // Mathematically sound position resolver avoiding center region & text overlap
        fun trySpawnWord(): DriftingWord? {
            val text = wordsList.random()
            val fontSize = (11..14).random().toFloat()
            val maxOpacity = (12..18).random().toFloat() / 100f
            val driftY = (25..50).random().toFloat()
            val duration = (4000..7000).random()

            for (attempt in 1..100) {
                val rx = (20..(screenWidth - 120)).random().toFloat()
                val ry = (40..(screenHeight - 80)).random().toFloat()

                // Center exclusion zone: 280dp wide x 320dp tall
                val inCenterWidth = rx > (screenWidth - 280) / 2f - 40f && rx < (screenWidth + 280) / 2f + 20f
                val inCenterHeight = ry > (screenHeight - 320) / 2f - 20f && ry < (screenHeight + 320) / 2f + 20f
                if (inCenterWidth && inCenterHeight) continue

                // Avoid overlap with other visible words
                val overlaps = activeWords.any { word ->
                    kotlin.math.abs(word.xDp - rx) < 110f && kotlin.math.abs(word.yDp - ry) < 40f
                }
                if (overlaps) continue

                return DriftingWord(
                    id = wordIdCounter++,
                    text = text,
                    xDp = rx,
                    yDp = ry,
                    fontSizeSp = fontSize,
                    driftYDp = driftY,
                    durationMs = duration,
                    maxOpacity = maxOpacity
                )
            }
            return null
        }

        fun spawnWordWithRemoval(delayMs: Long) {
            launch {
                delay(delayMs)
                trySpawnWord()?.let { word ->
                    activeWords.add(word)
                    launch {
                        delay(word.durationMs.toLong())
                        activeWords.remove(word)
                    }
                }
            }
        }

        // Stagger first 3 words spawning
        spawnWordWithRemoval(0)
        spawnWordWithRemoval(600)
        spawnWordWithRemoval(1400)

        // Maintain up to 5 words, checking periodically
        launch {
            while (true) {
                delay(1200)
                if (activeWords.size < 5) {
                    trySpawnWord()?.let { word ->
                        activeWords.add(word)
                        launch {
                            delay(word.durationMs.toLong())
                            activeWords.remove(word)
                        }
                    }
                }
            }
        }

        // --- Custom Animation Timeline Sequence ---
        // 0ms: Logo fades in + scales + slides up (600ms duration)
        launch {
            animLogoAlpha.animateTo(1f, animationSpec = tween(600, easing = EaseInOutCubic))
        }
        launch {
            animLogoScale.animateTo(1f, animationSpec = tween(600, easing = EaseInOutCubic))
        }
        launch {
            animLogoSlide.animateTo(0f, animationSpec = tween(600, easing = EaseInOutCubic))
        }

        // 300ms: App Name fades in + slides up (700ms duration)
        launch {
            delay(300)
            animAppAlpha.animateTo(1f, animationSpec = tween(700, easing = EaseInOutCubic))
        }
        launch {
            delay(300)
            animAppSlide.animateTo(0f, animationSpec = tween(700, easing = EaseInOutCubic))
        }

        // 600ms: Tagline fades in + slides up (700ms duration)
        launch {
            delay(600)
            animTaglineAlpha.animateTo(0.6f, animationSpec = tween(700, easing = EaseInOutCubic))
        }
        launch {
            delay(600)
            animTaglineSlide.animateTo(0f, animationSpec = tween(700, easing = EaseInOutCubic))
        }

        // 900ms: Loading track fades in and progress fill begins (900ms duration)
        launch {
            delay(900)
            animTrackAlpha.animateTo(0.08f, animationSpec = tween(350))
        }
        launch {
            delay(900)
            animProgress.animateTo(1f, animationSpec = tween(900, easing = EaseInOutCubic))
        }

        // 1900ms: Entire screen dissolve (600ms dissolve)
        launch {
            delay(1900)
            animScreenAlpha.animateTo(0f, animationSpec = tween(600, easing = LinearEasing))
            onFinished()
        }
    }

    // Programmatic Film Grain Noise Matrix
    val noiseBitmap = remember { createNoiseBitmap() }
    var grainOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    LaunchedEffect(Unit) {
        val random = java.util.Random()
        while (true) {
            delay(800)
            grainOffset = Offset(random.nextFloat() * 128f, random.nextFloat() * 128f)
        }
    }

    // Root Cinematic Canvas Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(animScreenAlpha.value)
    ) {
        // LAYER 2: Drifting Quotes Layer
        activeWords.forEach { word ->
            DriftingWordView(word = word)
        }

        // LAYER 3: Depth Vignette Overlay
        val backgroundThemeColor = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(x = size.width * 0.5f, y = size.height * 0.45f)
                    val radius = maxOf(size.width, size.height) * 0.8f
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.5f to Color.Transparent,
                                1.0f to backgroundThemeColor.copy(alpha = 0.85f)
                            ),
                            center = center,
                            radius = radius
                        )
                    )
                }
        )

        // LAYER 4: Center Splash Content (Animated Logo + Animated Typographies + Progress Bar)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-32).dp) // Positioned slightly above center vertically
        ) {
            // App Icon Quote Logo (Favicon) - Animated & Tinted
            val isDark = MaterialTheme.colorScheme.background == com.example.motivation.ui.theme.DarkBlack
            Icon(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = "Focus App Logo",
                tint = if (isDark) Color.White else Color(0xFFFC6E20), // `#FC6E20` in light mode
                modifier = Modifier
                    .size(64.dp)
                    .alpha(animLogoAlpha.value)
                    .scale(animLogoScale.value)
                    .offset(y = animLogoSlide.value.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // App Name Text
            Text(
                text = "FOCUS",
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Medium, // Literata serif weight 500
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.2.em,
                modifier = Modifier
                    .alpha(animAppAlpha.value)
                    .offset(y = animAppSlide.value.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tagline Text
            Text(
                text = "Small steps, Big change.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Light, // Inter sans-serif weight 300
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                letterSpacing = 0.05.em,
                modifier = Modifier
                    .alpha(animTaglineAlpha.value)
                    .offset(y = animTaglineSlide.value.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Unified Premium Loading Bar
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .alpha(if (animTrackAlpha.value > 0f) 1f else 0f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = animTrackAlpha.value))
            ) {
                if (animProgress.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animProgress.value)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onBackground,
                                        Color(0xFFFC6E20)
                                    )
                                )
                            )
                    )
                }
            }
        }

        // LAYER 5: Animated Film Grain Overlay (Topmost)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.035f)
        ) {
            val paint = Paint().apply {
                asFrameworkPaint().shader = BitmapShader(
                    noiseBitmap.asAndroidBitmap(),
                    Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT
                )
            }
            drawIntoCanvas { canvas ->
                canvas.translate(grainOffset.x, grainOffset.y)
                canvas.drawRect(
                    left = -256f,
                    top = -256f,
                    right = size.width + 256f,
                    bottom = size.height + 256f,
                    paint = paint
                )
            }
        }
    }
}

@Composable
fun DriftingWordView(word: DriftingWord) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(word.id) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = word.durationMs, easing = LinearEasing)
        )
    }

    val progress = animProgress.value
    val currentYOffset = -word.driftYDp * progress

    // Peak and fade curve: fade in at 20%, hold, fade out at 80%
    val opacity = when {
        progress < 0.2f -> (progress / 0.2f) * word.maxOpacity
        progress > 0.8f -> ((1f - progress) / 0.2f) * word.maxOpacity
        else -> word.maxOpacity
    }

    Box(
        modifier = Modifier
            .offset(x = word.xDp.dp, y = (word.yDp + currentYOffset).dp)
            .alpha(opacity)
            .blur(1.dp) // Soft blur for depth-of-field effect
    ) {
        Text(
            text = word.text,
            fontFamily = LiterataFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light,
            fontSize = word.fontSizeSp.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// Highly optimized noise generation bitmap creation method
private fun createNoiseBitmap(width: Int = 128, height: Int = 128): ImageBitmap {
    val config = Bitmap.Config.ARGB_8888
    val bitmap = Bitmap.createBitmap(width, height, config)
    val random = java.util.Random()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val noise = random.nextInt(256)
            val color = android.graphics.Color.argb(noise, 255, 255, 255)
            bitmap.setPixel(x, y, color)
        }
    }
    return bitmap.asImageBitmap()
}