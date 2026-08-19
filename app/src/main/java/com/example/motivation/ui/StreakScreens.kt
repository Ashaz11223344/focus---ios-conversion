@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.motivation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.R
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.InterFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.viewmodel.StreakViewModel
import com.example.motivation.viewmodel.StreakUiState

@Composable
fun StreakScreen(streakViewModel: StreakViewModel = viewModel(), onNavigateHome: () -> Unit) {
    val uiState by streakViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Daily Intent",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isQuoteCompletedToday) {
                    CompletedView(uiState = uiState)
                } else {
                    QuoteCommitView(uiState = uiState, viewModel = streakViewModel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Back to Explore",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun QuoteCommitView(uiState: StreakUiState, viewModel: StreakViewModel) {
    if (uiState.showSuccessAnimation) {
        SuccessAnimation()
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Daily Reflection Intent",
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Large faint quote mark in the background
                    Icon(
                        painter = painterResource(id = R.drawable.ic_quote_mark),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        modifier = Modifier
                            .size(130.dp)
                            .align(Alignment.Center)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_quote_mark),
                            contentDescription = null,
                            tint = VibrantOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "“${uiState.requiredQuote.text}”",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 28.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (uiState.requiredQuote.category.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.requiredQuote.category.uppercase(),
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = VibrantOrange
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.completeDailyQuote() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Commit to Intent",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CompletedView(uiState: StreakUiState) {
    val flameGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF3D00), // Red-Orange
            Color(0xFFFC6E20), // Vibrant Orange
            Color(0xFFFFC107)  // Yellow-Gold
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(130.dp)
        ) {
            // Glow layer
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFC6E20).copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            // Icon with Gradient blending
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = flameGradient, blendMode = BlendMode.SrcAtop)
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_streak),
                    contentDescription = "Streak flame",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Intent Affirmed!",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You've successfully completed your daily reflection.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "${uiState.streakCount}",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp,
                    color = VibrantOrange,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = if (uiState.streakCount == 1) "DAY STREAK" else "DAYS STREAK",
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SuccessAnimation() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }
    
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = VibrantOrange.copy(alpha = 0.15f * scale.value),
                radius = 80.dp.toPx() * scale.value
            )
            drawCircle(
                color = VibrantOrange.copy(alpha = 0.3f * (1f - scale.value)),
                radius = 70.dp.toPx() * scale.value,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Success",
            modifier = Modifier.size((80 * scale.value).dp),
            tint = VibrantOrange
        )
    }
}
