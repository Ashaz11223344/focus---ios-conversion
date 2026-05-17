package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.viewmodel.MainViewModel
import com.example.motivation.viewmodel.MoodStats

@Composable
fun MoodScreen(mainViewModel: MainViewModel = viewModel()) {
    val moods = listOf(
        MoodData("Happy", Icons.Rounded.SentimentVerySatisfied, 5),
        MoodData("Inspired", Icons.Rounded.AutoAwesome, 5),
        MoodData("Calm", Icons.Rounded.SelfImprovement, 4),
        MoodData("Neutral", Icons.Rounded.SentimentNeutral, 3),
        MoodData("Sad", Icons.Rounded.SentimentVeryDissatisfied, 2),
        MoodData("Tired", Icons.Rounded.Bedtime, 2),
        MoodData("Angry", Icons.Rounded.Whatshot, 1),
        MoodData("Anxious", Icons.Rounded.Psychology, 1)
    )
    
    val stats by mainViewModel.monthlyMoodStats.collectAsState()
    var selectedMood by remember { mutableStateOf<MoodData?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Colors from palette
    val orange = Color(0xFFFC6E20)
    val cream = Color(0xFFFFE7D0)
    val charcoalGray = Color(0xFF323232)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val columns = when {
        screenWidthDp > 840 -> 4
        screenWidthDp > 600 -> 3
        else -> 2
    }
    val gridHeight = when (columns) {
        4 -> 360.dp
        3 -> 520.dp
        else -> 700.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Mood Tracker",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 32.sp
                ),
                color = cream,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Analytics Card
            stats?.let { moodStats ->
                MoodAnalyticsCard(moodStats, orange, charcoalGray, cream)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "How are you feeling?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = cream,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(gridHeight),
                userScrollEnabled = false
            ) {
                items(moods) { mood ->
                    val isSelected = selectedMood == mood
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .clickable { selectedMood = mood },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) orange else charcoalGray
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = mood.icon,
                                contentDescription = mood.name,
                                modifier = Modifier.size(48.dp),
                                tint = if (isSelected) Color.White else orange
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = mood.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else cream
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedMood != null) {
                Button(
                    onClick = { 
                        selectedMood?.let { 
                            // Map icon name to emoji or similar for DB storage
                            val iconId = it.name.lowercase()
                            mainViewModel.addMoodEntry(it.name, iconId, it.value)
                            selectedMood = null
                            showSuccessMessage = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)
                ) {
                    Text(
                        "Log Mood", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            if (showSuccessMessage) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Mood logged successfully!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = orange,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSuccessMessage = false
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MoodAnalyticsCard(stats: MoodStats, orange: Color, cardBg: Color, textColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "This Month's Mood",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Average Score",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        String.format("%.1f / 5.0", stats.averageScore),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        ),
                        color = orange
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Most Frequent",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stats.mostFrequentMood,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = (stats.averageScore / 5.0).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = orange,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

data class MoodData(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val value: Int)
