package com.example.motivation.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.motivation.MainActivity
import com.example.motivation.R
import com.example.motivation.ui.theme.DarkBlack
import com.example.motivation.ui.theme.CreamBeige
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.InterFontFamily
import com.example.motivation.ui.theme.PlaywriteGBSFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    isRestart: Boolean,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val reduceMotion = rememberReduceMotion()

    // Gesture Swipe detection offset
    var dragOffsetX by remember { mutableStateOf(0f) }

    // Intercept Back press
    BackHandler(enabled = currentScreen == 0) {
        viewModel.completeOnboarding()
        onFinished()
    }
    BackHandler(enabled = currentScreen > 0) {
        viewModel.prevScreen()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlack)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffsetX > 150f) {
                            // Swipe right -> Previous Screen
                            viewModel.prevScreen()
                        } else if (dragOffsetX < -150f) {
                            // Swipe left -> Next Screen
                            if (currentScreen < 7) {
                                viewModel.nextScreen()
                            }
                        }
                        dragOffsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX += dragAmount
                    }
                )
            }
            .safeDrawingPadding()
    ) {
        // --- Top Right Skip Button ---
        if (currentScreen < 7) {
            TextButton(
                onClick = { viewModel.skipToLastScreen() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = CreamBeige.copy(alpha = 0.6f)
                )
            }
        }

        // --- Main Sliding Content Container ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val isNext = targetState > initialState
                    if (reduceMotion) {
                        fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                    } else {
                        val slideEasing = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)
                        if (isNext) {
                            (slideInHorizontally(animationSpec = tween(350, easing = slideEasing), initialOffsetX = { it }) + 
                             fadeIn(animationSpec = tween(350))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(350, easing = slideEasing), targetOffsetX = { -it }) + 
                             fadeOut(animationSpec = tween(350)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(350, easing = slideEasing), initialOffsetX = { -it }) + 
                             fadeIn(animationSpec = tween(350))) togetherWith
                            (slideOutHorizontally(animationSpec = tween(350, easing = slideEasing), targetOffsetX = { it }) + 
                             fadeOut(animationSpec = tween(350)))
                        }
                    }
                },
                label = "OnboardingScreenTransition"
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> ScreenWelcome(onNext = { viewModel.nextScreen() })
                    1 -> ScreenDailyQuotes(onNext = { viewModel.nextScreen() })
                    2 -> ScreenMoodJournal(onNext = { viewModel.nextScreen() })
                    3 -> ScreenStreaksBadges(onNext = { viewModel.nextScreen() })
                    4 -> ScreenAchievementsShowcase(onNext = { viewModel.nextScreen() })
                    5 -> ScreenNotificationsFinish(
                        viewModel = viewModel,
                        onNext = { viewModel.nextScreen() }
                    )
                    6 -> Screen6PowerFeatures(
                        viewModel = viewModel,
                        onNext = { viewModel.nextScreen() },
                        onFinished = {
                            viewModel.completeOnboarding()
                            onFinished()
                        }
                    )
                    7 -> FocusGuardBlockOnboardingPage(
                        onFinish = {
                            viewModel.completeOnboarding()
                            onFinished()
                        },
                        onSkip = {
                            viewModel.completeOnboarding()
                            onFinished()
                        }
                    )
                }
            }
        }

        // --- Bottom Navigation Dots ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 8) {
                    val isActive = i <= currentScreen
                    Box(
                        modifier = Modifier
                            .size(if (i == currentScreen) 10.dp else 8.dp)
                            .background(
                                color = if (isActive) VibrantOrange else CreamBeige.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1 - WELCOME
// ==========================================
@Composable
private fun ScreenWelcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        AnimatedEntrance(index = 0) {
            LogoPulse {
                Icon(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "Focus logo",
                    tint = VibrantOrange,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedEntrance(index = 1) {
            Text(
                text = "Welcome to Focus.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(index = 2) {
            Text(
                text = "Your personal space for daily quotes,\nmood tracking, and quiet reflection.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedEntrance(index = 3) {
            Text(
                text = "100% offline. No account. No tracking.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = VibrantOrange,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedEntrance(index = 4) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .widthIn(max = 280.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Get Started →",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlack
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ==========================================
// SCREEN 2 - DAILY QUOTES
// ==========================================
@Composable
private fun ScreenDailyQuotes(onNext: () -> Unit) {
    val reduceMotion = rememberReduceMotion()
    
    // Float animation for mock card
    val floatOffset = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            floatOffset.animateTo(
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Premium Quote Card Preview
        AnimatedEntrance(index = 0, modifier = Modifier.offset(y = floatOffset.value.dp)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .widthIn(max = 320.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                border = BorderStroke(1.dp, VibrantOrange.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_quote_mark),
                        contentDescription = null,
                        tint = VibrantOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Quiet the mind, and the soul will speak.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = PlaywriteGBSFontFamily,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            fontStyle = FontStyle.Italic
                        ),
                        textAlign = TextAlign.Center,
                        color = CreamBeige
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Focus",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp
                        ),
                        color = CreamBeige.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedEntrance(index = 1) {
            Text(
                text = "Start every day with intention.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(index = 2) {
            Text(
                text = "A fresh handpicked quote greets you\nevery morning — in beautiful calligraphy.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedEntrance(index = 3) {
            Text(
                text = "Swipe. Refresh. Save your favorites.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = VibrantOrange,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedEntrance(index = 4) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .widthIn(max = 280.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Next →",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlack
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ==========================================
// SCREEN 3 - MOOD & JOURNAL
// ==========================================
@Composable
private fun ScreenMoodJournal(onNext: () -> Unit) {
    val moods = listOf(
        Icons.Rounded.SentimentVeryDissatisfied,
        Icons.Rounded.SentimentDissatisfied,
        Icons.Rounded.SentimentNeutral,
        Icons.Rounded.SentimentSatisfied,
        Icons.Rounded.SentimentVerySatisfied
    )
    var journalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Delay the journal icon appearing until mood icon springs finish
        delay(moods.size * 80L + 200L)
        journalVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Mood icons row with springs
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            moods.forEachIndexed { index, icon ->
                MoodIconSpring(index = index) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF242424), shape = CircleShape)
                            .border(BorderStroke(1.dp, Color(0xFF383838)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = VibrantOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Journal Icon fading in
        AnimatedVisibility(
            visible = journalVisible,
            enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF242424), shape = RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, VibrantOrange.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Book,
                    contentDescription = null,
                    tint = VibrantOrange,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedEntrance(index = 1) {
            Text(
                text = "Know yourself better.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(index = 2) {
            Text(
                text = "Log your mood once a day and write\nfreely in your private journal.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedEntrance(index = 3) {
            Text(
                text = "Everything stays on your device.\nNobody else can ever read this.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = VibrantOrange,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedEntrance(index = 4) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .widthIn(max = 280.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Next →",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlack
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ==========================================
// SCREEN 4 - STREAKS & BADGES
// ==========================================
@Composable
private fun ScreenStreaksBadges(onNext: () -> Unit) {
    var countFinished by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Visual Layout with Streak count ticking up and Badge sparkle
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Streak Counter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatingStreakCounter(
                        textStyle = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp,
                            fontFamily = LiterataFontFamily
                        ),
                        color = VibrantOrange,
                        onFinished = { countFinished = true }
                    )
                    Text(
                        text = "DAY STREAK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp
                        ),
                        color = CreamBeige.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))

                // Badge with Sparkle
                Box(contentAlignment = Alignment.Center) {
                    BadgeSparkle(
                        modifier = Modifier.size(100.dp),
                        trigger = countFinished
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_achievement_7),
                        contentDescription = "Streak Badge",
                        tint = if (countFinished) VibrantOrange else CreamBeige.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedEntrance(index = 1) {
            Text(
                text = "Show up every day.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(index = 2) {
            Text(
                text = "Build streaks, unlock achievement badges,\nand watch your consistency compound\ninto something real.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedEntrance(index = 3) {
            Text(
                text = "Small steps. Big change.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = VibrantOrange,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedEntrance(index = 4) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .widthIn(max = 280.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Next →",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlack
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ==========================================
// SCREEN 5 - NOTIFICATIONS & FINISH
// ==========================================
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ScreenNotificationsFinish(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()

    // Determine current notification permission status
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Register Android permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        // If granted or denied, let user proceed
    }

    // Notification mockup slide down animation
    val slideOffset = remember { Animatable(-100f) }
    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            slideOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = EaseOutBack)
            )
        } else {
            slideOffset.snapTo(0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Phone notification mockup
        AnimatedEntrance(index = 0, modifier = Modifier.offset(y = slideOffset.value.dp)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                border = BorderStroke(1.dp, Color(0xFF383838))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(VibrantOrange, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = null,
                            tint = DarkBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Focus",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                ),
                                color = CreamBeige
                            )
                            Text(
                                text = "now",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = CreamBeige.copy(alpha = 0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Start where you are. Use what you have.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontStyle = FontStyle.Italic
                            ),
                            color = CreamBeige.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedEntrance(index = 1) {
            Text(
                text = "Let Focus remind you.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(index = 2) {
            Text(
                text = "Get daily quote notifications at\nthe exact time that works for you.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedEntrance(index = 3) {
            Text(
                text = "You control the schedule.\nYou control the quiet hours.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = VibrantOrange,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // If notification permission not yet granted, show permission buttons
                Button(
                    onClick = {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .widthIn(max = 280.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "Enable Notifications",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlack
                        )
                    )
                }

                TextButton(
                    onClick = {
                        onNext()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = "Maybe Later",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = CreamBeige.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Permission already granted or SDK older than Tiramisu
                Divider(
                    color = CreamBeige.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 280.dp)
                        .padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        onNext()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .widthIn(max = 280.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Next →",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlack
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun Screen6PowerFeatures(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
    onFinished: () -> Unit
) {
    var timeMs by remember { mutableStateOf(0) }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            timeMs = 5000
            return@LaunchedEffect
        }
        while (true) {
            timeMs = 0
            delay(400)
            timeMs = 400
            delay(300) // 700ms
            timeMs = 700
            delay(200) // 900ms
            timeMs = 900
            delay(400) // 1300ms
            timeMs = 1300
            delay(400) // 1700ms
            timeMs = 1700
            delay(400) // 2100ms
            timeMs = 2100
            delay(2900) // 5000ms loop
            timeMs = 5000
        }
    }

    val showCard1Selected = timeMs >= 900
    val showCard2Selected = timeMs >= 1700
    val showCheckboxes = timeMs >= 700
    val showActionButtons = timeMs >= 2100

    val scaleCard1 by animateFloatAsState(
        targetValue = if (showCard1Selected) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val scaleCard2 by animateFloatAsState(
        targetValue = if (showCard2Selected) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val checkboxesAlpha by animateFloatAsState(
        targetValue = if (showCheckboxes) 1f else 0f,
        animationSpec = tween(300)
    )

    val actionButtonsAlpha by animateFloatAsState(
        targetValue = if (showActionButtons) 1f else 0f,
        animationSpec = tween(400)
    )

    val showRipple1 = timeMs in 400..699
    val ripple1Alpha by animateFloatAsState(
        targetValue = if (showRipple1) 0.8f else 0f,
        animationSpec = tween(200)
    )
    val ripple1Scale by animateFloatAsState(
        targetValue = if (showRipple1) 1.2f else 0.4f,
        animationSpec = tween(300)
    )

    val showRipple2 = timeMs in 1300..1599
    val ripple2Alpha by animateFloatAsState(
        targetValue = if (showRipple2) 0.8f else 0f,
        animationSpec = tween(200)
    )
    val ripple2Scale by animateFloatAsState(
        targetValue = if (showRipple2) 1.2f else 0.4f,
        animationSpec = tween(300)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.4f))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = BorderStroke(1.dp, Color(0xFF2E2E2E))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (timeMs >= 900) Icons.Rounded.Close else Icons.Rounded.Menu,
                        contentDescription = null,
                        tint = CreamBeige,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when {
                            timeMs >= 1700 -> "2 selected"
                            timeMs >= 900 -> "1 selected"
                            else -> "Focus"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        ),
                        color = CreamBeige
                      )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.alpha(actionButtonsAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = VibrantOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = CreamBeige,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        tint = CreamBeige,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleCard1),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                    border = BorderStroke(
                        width = if (showCard1Selected) 2.dp else 1.dp,
                        color = if (showCard1Selected) VibrantOrange else Color(0xFF383838)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (showCard1Selected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(VibrantOrange.copy(alpha = 0.08f))
                            )
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Start where you are. Use what you have.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 14.sp
                                ),
                                color = CreamBeige
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Focus",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp
                                ),
                                color = CreamBeige.copy(alpha = 0.5f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(18.dp)
                                .alpha(checkboxesAlpha)
                        ) {
                            if (showCard1Selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = VibrantOrange,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.dp, CreamBeige.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(ripple1Scale)
                        .alpha(ripple1Alpha)
                        .background(VibrantOrange.copy(alpha = 0.4f), CircleShape)
                        .border(2.dp, VibrantOrange, CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleCard2),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                    border = BorderStroke(
                        width = if (showCard2Selected) 2.dp else 1.dp,
                        color = if (showCard2Selected) VibrantOrange else Color(0xFF383838)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (showCard2Selected) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(VibrantOrange.copy(alpha = 0.08f))
                            )
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Quiet the mind, and the soul will speak.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 14.sp
                                ),
                                color = CreamBeige
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Focus",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp
                                ),
                                color = CreamBeige.copy(alpha = 0.5f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(18.dp)
                                .alpha(checkboxesAlpha)
                        ) {
                            if (showCard2Selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = VibrantOrange,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.dp, CreamBeige.copy(alpha = 0.4f), CircleShape)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(ripple2Scale)
                        .alpha(ripple2Alpha)
                        .background(VibrantOrange.copy(alpha = 0.4f), CircleShape)
                        .border(2.dp, VibrantOrange, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedEntrance(index = 1) {
            Text(
                text = "More ways to use your quotes.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedEntrance(index = 2) {
            Text(
                text = "Double tap any quote to start selecting — then favorite, share, or copy multiple quotes all at once.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedEntrance(index = 3) {
            Text(
                text = "Available everywhere:\nQuotes · Favorites · History · Search",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = VibrantOrange,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.6f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .widthIn(max = 280.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlack
                    )
                )
            }
        }
    }
}

@Composable
fun FocusGuardSilenceOnboardingPage(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bell_pulse")
    val pulseRadius1 by infiniteTransition.animateFloat(
        initialValue = 44.dp.value,
        targetValue = 70.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha1"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // == BELL DRAWN IN CANVAS WITH PULSING WAVE ==
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsing dampening wave
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFFC6E20).copy(alpha = pulseAlpha1),
                        radius = pulseRadius1 * density,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }

                // Inner circle background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFC6E20).copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color(0xFFFC6E20).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = Color(0xFFFC6E20),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // == TITLE ==
            Text(
                text = "Peace of mind, scheduled.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 36.sp
                ),
                color = Color(0xFFF7F4EF),
                textAlign = TextAlign.Center
            )

            // == SUBTITLE ==
            Text(
                text = "Set Do Not Disturb schedules for sleep, work, or study. Focus Guard silences incoming notifications automatically.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = Color(0xFFF7F4EF).copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            // == FEATURE CARDS ==
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Smart Quiet Hours",
                    subtitle = "Schedules trigger DND"
                )
                FeatureMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.VolumeMute,
                    title = "No Distractions",
                    subtitle = "System-level silence"
                )
            }

            // == MOCK DND SCHEDULE CARD ==
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E1E1E),
                border = BorderStroke(1.dp, Color(0xFF2A2A2A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Morning Meditation",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFFF7F4EF)
                        )
                        Text(
                            text = "07:00 – 08:00 (Mon, Wed, Fri)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFFF7F4EF).copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        tint = Color(0xFFFC6E20),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // == PRIMARY BUTTON ==
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC6E20))
            ) {
                Text(
                    text = "Next →",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }

            // == SKIP ==
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontSize = 14.sp
                    ),
                    color = Color(0xFFF7F4EF).copy(alpha = 0.45f)
                )
            }
        }
    }
}

@Composable
fun FocusGuardBlockOnboardingPage(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val shieldGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val reduceMotion = rememberReduceMotion()
    val lockScale by if (reduceMotion) {
        rememberUpdatedState(1f)
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lock_scale"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // == SHIELD DRAWN IN CANVAS ==
            AnimatedEntrance(index = 0) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .size(88.dp)
                            .scale(shieldScale)
                    ) {
                        val w = size.width
                        val h = size.height
                        // Draw shield shape using a Path
                        val shieldPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.5f, 0f)
                            lineTo(w * 0.95f, h * 0.18f)
                            lineTo(w * 0.95f, h * 0.52f)
                            cubicTo(w * 0.95f, h * 0.78f, w * 0.72f, h * 0.93f, w * 0.5f, h)
                            cubicTo(w * 0.28f, h * 0.93f, w * 0.05f, h * 0.78f, w * 0.05f, h * 0.52f)
                            lineTo(w * 0.05f, h * 0.18f)
                            close()
                        }
                        drawPath(
                            path = shieldPath,
                            color = Color(0xFFFC6E20).copy(alpha = shieldGlow)
                        )
                        // Inner checkmark
                        val checkPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.28f, h * 0.50f)
                            lineTo(w * 0.44f, h * 0.64f)
                            lineTo(w * 0.70f, h * 0.37f)
                        }
                        drawPath(
                            path = checkPath,
                            color = Color(0xFF121212),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = w * 0.07f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            // == TITLE ==
            AnimatedEntrance(index = 1) {
                Text(
                    text = "Reclaim your focus.",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 36.sp
                    ),
                    color = Color(0xFFF7F4EF),
                    textAlign = TextAlign.Center
                )
            }

            // == SUBTITLE ==
            AnimatedEntrance(index = 2) {
                Text(
                    text = "Block distracting apps on your own terms. Try to open them, and Focus Guard blocks them instantly.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = Color(0xFFF7F4EF).copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }

            // == TWO FEATURE CARDS ==
            AnimatedEntrance(index = 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureMiniCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Block,
                        title = "App blocker",
                        subtitle = "Block selected apps"
                    )
                    FeatureMiniCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Lock,
                        title = "Secure overlay",
                        subtitle = "Full-screen blockade"
                    )
                }
            }

            // == MOCK BLOCKED APP CARD ==
            AnimatedEntrance(index = 4) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1E1E),
                    border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF242424), shape = RoundedCornerShape(10.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF383838)), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Apps,
                                        contentDescription = null,
                                        tint = CreamBeige.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "Social Media",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = CreamBeige
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = VibrantOrange,
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(lockScale)
                            )
                        }
                        
                        Divider(
                            color = Color(0xFF2E2E2E),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "BLOCKED UNTIL 6:00 PM",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            ),
                            color = VibrantOrange,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // == ITALIC QUOTE ==
            AnimatedEntrance(index = 5) {
                Text(
                    text = "\"The key is not to prioritize what's on your schedule,\nbut to schedule your priorities.\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = PlaywriteGBSFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFFF7F4EF).copy(alpha = 0.42f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // == PRIMARY BUTTON ==
            AnimatedEntrance(index = 6) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .widthIn(max = 280.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlack
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureMiniCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF2A2A2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFC6E20),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = Color(0xFFF7F4EF),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = LiterataFontFamily,
                    fontSize = 11.sp
                ),
                color = Color(0xFFF7F4EF).copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScreenAchievementsShowcase(onNext: () -> Unit) {
    val reduceMotion = rememberReduceMotion()

    // 1. Accessibility Aligned Entrance Alphas and Scales
    val pfpAlpha = remember { Animatable(0f) }
    val badgeAlpha = remember { Animatable(0f) }
    val pfpScale = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            launch {
                pfpAlpha.animateTo(1f, animationSpec = tween(400))
            }
            launch {
                badgeAlpha.animateTo(1f, animationSpec = tween(400))
            }
            pfpScale.snapTo(1f)
            badgeScale.snapTo(1f)
        } else {
            pfpAlpha.snapTo(1f)
            badgeAlpha.snapTo(1f)
            pfpScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            delay(200L)
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // 2. Continuous Pulse for Badge (if motion not reduced)
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseScale by if (reduceMotion) {
        rememberUpdatedState(1f)
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Component (Profile Picture & Badge Overlay)
        Box(
            modifier = Modifier
                .size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Large Profile Picture Container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(
                        scaleX = pfpScale.value,
                        scaleY = pfpScale.value,
                        alpha = pfpAlpha.value
                    )
                    .background(VibrantOrange, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Mock avatar silhouette
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = DarkBlack,
                    modifier = Modifier.size(80.dp)
                )
            }

            // Overlaid Badge (Bottom-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-12).dp, y = (-12).dp)
                    .graphicsLayer(
                        scaleX = badgeScale.value * pulseScale,
                        scaleY = badgeScale.value * pulseScale,
                        alpha = badgeAlpha.value
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(48.dp)) {
                    val widthPx = size.width
                    val heightPx = size.height
                    val radiusPx = widthPx / 2f - 6.dp.toPx()

                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.parseColor("#8B008B")
                            setShadowLayer(
                                20f,
                                0f,
                                0f,
                                android.graphics.Color.parseColor("#FF4081")
                            )
                        }
                        canvas.nativeCanvas.drawCircle(
                            widthPx / 2f,
                            heightPx / 2f,
                            radiusPx,
                            paint
                        )
                    }
                    // Radial gradient brush
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF4081), Color(0xFF8B008B), Color(0xFF4A0E4E))
                        ),
                        radius = radiusPx
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Subtitle/Privacy Note (SQLite, Offline)
        Text(
            text = stringResource(id = R.string.ob_achievements_sub),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            color = VibrantOrange,
            textAlign = TextAlign.Center
        )

        // Description Copy
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ob_achievements_title),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = CreamBeige,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.ob_achievements_desc),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = CreamBeige.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Static Milestone progression previews (Bronze, Silver, Gold outlines)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bronze Milestone Card
            MilestoneOutlineCard(
                icon = Icons.Rounded.Star,
                outlineColor = Color(0xFFCD7F32), // Bronze
                title = "Bronze"
            )
            // Silver Milestone Card
            MilestoneOutlineCard(
                icon = Icons.Rounded.StarBorder,
                outlineColor = Color(0xFFC0C0C0), // Silver
                title = "Silver"
            )
            // Gold Milestone Card
            MilestoneOutlineCard(
                icon = Icons.Rounded.EmojiEvents,
                outlineColor = Color(0xFFFFD700), // Gold
                title = "Gold"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Continue Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .widthIn(max = 280.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ob_achievements_next),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlack
                )
            )
        }
    }
}

@Composable
private fun MilestoneOutlineCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    outlineColor: Color,
    title: String
) {
    Card(
        modifier = Modifier.size(width = 80.dp, height = 90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = outlineColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = CreamBeige.copy(alpha = 0.6f)
            )
        }
    }
}
