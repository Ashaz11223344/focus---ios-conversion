package com.example.motivation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.motivation.R
import com.example.motivation.ui.theme.CreamBeige
import com.example.motivation.ui.theme.DarkBlack
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, userName: String) {
    val currentUserName by rememberUpdatedState(userName)

    // Entrance alpha animation
    val animateAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Sleek loading progress animation
    val progressAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = EaseInOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        // Trigger quick smooth fade-in entrance animation
        animateAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
        )

        // Hold splash screen for exactly 1 second, then route cleanly
        delay(1000)
        if (currentUserName.isBlank()) {
            navController.navigate("name_input") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("quotes") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBlack,
                        Color(0xFF151515),
                        Color(0xFF0F0F0F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .alpha(animateAlpha.value)
        ) {
            // Stunning logo layout with soft orange neon glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        spotColor = VibrantOrange.copy(alpha = 0.5f),
                        ambientColor = VibrantOrange.copy(alpha = 0.3f)
                    )
                    .background(Color(0xFF1C1C1B), CircleShape)
                    .border(1.5.dp, Brush.radialGradient(
                        colors = listOf(VibrantOrange, Color.Transparent),
                        radius = 300f
                    ), CircleShape)
                    .padding(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.logo_foreground),
                    contentDescription = "Focus App Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium app title in Literata font
            Text(
                text = "Focus",
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = CreamBeige,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Premium subtitle in Literata font
            Text(
                text = "Small steps. Big change.",
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = CreamBeige.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sleek premium progress indicator line
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .background(Color(0xFF2D2D2D), shape = CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(VibrantOrange, Color(0xFFFF9E66))
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
