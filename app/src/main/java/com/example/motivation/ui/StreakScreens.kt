@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.motivation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.model.Achievement
import com.example.motivation.viewmodel.AffirmationState
import com.example.motivation.viewmodel.StreakViewModel
import com.example.motivation.viewmodel.StreakUiState
import kotlinx.coroutines.launch

@Composable
fun StreakScreen(streakViewModel: StreakViewModel = viewModel(), onNavigateHome: () -> Unit) {
    val uiState by streakViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isAffirmationCompletedToday) {
            CompletedView(uiState = uiState)
        } else {
            AffirmationView(uiState = uiState, viewModel = streakViewModel)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateHome) { Text("Back to Quotes") }
    }
}

@Composable
fun AffirmationView(uiState: StreakUiState, viewModel: StreakViewModel) {
    // Animation for incorrect attempts
    val shake = remember { Animatable(0f) }
    LaunchedEffect(uiState.affirmationState) {
        if (uiState.affirmationState == AffirmationState.INCORRECT) {
            for (i in 0..5) {
                shake.animateTo(if (i % 2 == 0) 15f else -15f, animationSpec = tween(50))
            }
            shake.animateTo(0f, animationSpec = tween(50))
        }
    }

    if (uiState.affirmationState == AffirmationState.CORRECT) {
        SuccessAnimation()
    } else {
        Text("Your daily intent:", style = MaterialTheme.typography.titleMedium)
        Text(
            text = uiState.requiredAffirmation.sentence,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        OutlinedTextField(
            value = uiState.userTypedAffirmation,
            onValueChange = { viewModel.onUserTyped(it) },
            label = { Text("Type the sentence to affirm") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = shake.value.dp),
            isError = uiState.affirmationState == AffirmationState.INCORRECT,
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.completeAffirmation() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.userTypedAffirmation.isNotBlank()
        ) {
            Text("Confirm Intent")
        }
    }
}

@Composable
fun CompletedView(uiState: StreakUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Intent affirmed for today!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Current Streak: ${uiState.streakCount} days", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun SuccessAnimation() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 500))
    }
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Success",
        modifier = Modifier.size((120 * scale.value).dp),
        tint = Color(0xFF4CAF50)
    )
}

@Composable
fun AchievementsScreen(streakViewModel: StreakViewModel = viewModel(), onNavigateHome: () -> Unit) {
    val uiState by streakViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.achievements) { achievement ->
                AchievementCard(achievement = achievement, currentStreak = uiState.streakCount)
            }
        }
        Button(onClick = onNavigateHome, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)) {
            Text("Back to Quotes")
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, currentStreak: Int) {
    val progress by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else (currentStreak.toFloat() / achievement.streakRequired.toFloat()),
        animationSpec = tween(1000),
        label = "progressAnimation"
    )

    // Get the color outside the Canvas
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                if (achievement.isUnlocked) {
                    Icon(
                        painter = painterResource(id = achievement.iconResId),
                        contentDescription = achievement.title,
                        modifier = Modifier.size(64.dp),
                        tint = primaryColor // Use the variable here
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.LightGray,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8f)
                        )
                        drawArc(
                            color = primaryColor, // Use the variable here
                            startAngle = -90f,
                            sweepAngle = 360 * progress,
                            useCenter = false,
                            style = Stroke(width = 8f)
                        )
                    }
                    Text("${(progress * 100).toInt()}%", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = achievement.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (achievement.isUnlocked) achievement.description else "${achievement.streakRequired} days",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
