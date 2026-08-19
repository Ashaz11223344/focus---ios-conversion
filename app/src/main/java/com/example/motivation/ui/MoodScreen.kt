package com.example.motivation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import java.text.SimpleDateFormat
import java.util.Locale
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
import androidx.navigation.NavController
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.viewmodel.MainViewModel
import com.example.motivation.viewmodel.MoodStats
import java.util.Calendar

@Composable
fun MoodScreen(
    navController: NavController? = null,
    mainViewModel: MainViewModel = viewModel()
) {
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
    val todayMood by mainViewModel.todayMoodEntry.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedMood by remember { mutableStateOf<MoodData?>(null) }
    var showPartialWarningDialog by remember { mutableStateOf(false) }
    var showMoodHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(todayMood) {
        todayMood?.let { logged ->
            selectedMood = moods.firstOrNull { it.name.equals(logged.moodName, ignoreCase = true) }
        }
    }

    // Theme-aware colors
    val orange = VibrantOrange
    val cardBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val onBgColor = MaterialTheme.colorScheme.onBackground

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val columns = when {
        screenWidthDp > 840 -> 4
        screenWidthDp > 600 -> 3
        else -> 2
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
                color = onBgColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Analytics Card
            stats?.let { moodStats ->
                MoodAnalyticsCard(moodStats, orange, cardBg, textColor, textMuted)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Weekly Wrapped Launcher Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val isSunday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        if (isSunday) {
                            navController?.navigate("weekly_report/false")
                        } else {
                            showPartialWarningDialog = true
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, orange.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(orange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", fontSize = 24.sp, color = orange)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly Focus Wrapped",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = LiterataFontFamily
                            ),
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate your weekly mood & journal summary",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Go",
                        tint = orange
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mood History Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showMoodHistoryDialog = true
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, orange.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(orange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "History",
                            tint = orange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mood History ✦",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = LiterataFontFamily
                            ),
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Browse your complete emotional journey",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Go",
                        tint = orange
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mood History Dialog
            if (showMoodHistoryDialog) {
                AlertDialog(
                    onDismissRequest = { showMoodHistoryDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = orange,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Your Mood Journey",
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    },
                    text = {
                        val allEntries by mainViewModel.moodEntries.collectAsState()
                        if (allEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No logged mood entries yet ✦",
                                    fontFamily = LiterataFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    color = textMuted
                                )
                            }
                        } else {
                            val sortedEntries = allEntries.sortedByDescending { it.timestamp }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(sortedEntries) { entry ->
                                    val dateStr = remember(entry.timestamp) {
                                        val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                                        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                                        sdf.format(cal.time)
                                    }
                                    
                                    val moodIcon = when (entry.moodName.lowercase(Locale.ROOT)) {
                                        "happy" -> Icons.Rounded.SentimentVerySatisfied
                                        "inspired" -> Icons.Rounded.AutoAwesome
                                        "calm" -> Icons.Rounded.SelfImprovement
                                        "neutral" -> Icons.Rounded.SentimentNeutral
                                        "sad" -> Icons.Rounded.SentimentVeryDissatisfied
                                        "tired" -> Icons.Rounded.Bedtime
                                        "angry" -> Icons.Rounded.Whatshot
                                        "anxious" -> Icons.Rounded.Psychology
                                        else -> Icons.Rounded.SentimentNeutral
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = moodIcon,
                                                contentDescription = entry.moodName,
                                                tint = orange,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = entry.moodName.replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = LiterataFontFamily
                                                    ),
                                                    color = textColor
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = dateStr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = textMuted
                                                )
                                            }
                                            Text(
                                                text = "${entry.moodValue}/5",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = LiterataFontFamily
                                                ),
                                                color = orange
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showMoodHistoryDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = orange)
                        ) {
                            Text("Done", color = Color.White)
                        }
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // Partial Week Warning Dialog
            if (showPartialWarningDialog) {
                val todayCalendar = Calendar.getInstance()
                val daysLeft = if (todayCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    0
                } else {
                    Calendar.SATURDAY - todayCalendar.get(Calendar.DAY_OF_WEEK) + 1
                }
                AlertDialog(
                    onDismissRequest = { showPartialWarningDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = null,
                                tint = orange,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Week isn't over yet",
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "You still have $daysLeft day${if (daysLeft == 1) "" else "s"} left this week. This will be a partial snapshot of your journey so far.",
                            fontFamily = LiterataFontFamily,
                            color = textMuted
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showPartialWarningDialog = false
                                navController?.navigate("weekly_report/true")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = orange)
                        ) {
                            Text("Generate Anyway →", color = Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showPartialWarningDialog = false },
                            border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Text("Cancel")
                        }
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Text(
                text = "How are you feeling?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = onBgColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (todayMood != null) {
                Text(
                    text = "You've already logged your mood today. You can update it.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    color = orange,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Chunk moods into rows based on screen width columns. This removes the nested unscrollable LazyVerticalGrid 
            // with a hardcoded height, preventing items from being cut off on foldable/flip phones or varying aspect ratios.
            moods.chunked(columns).forEach { rowMoods ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowMoods.forEach { mood ->
                        val isSelected = selectedMood == mood
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .clickable { selectedMood = mood },
                            shape = RoundedCornerShape(24.dp),
                            border = if (isSelected) BorderStroke(2.dp, orange) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) orange else cardBg
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
                                    color = if (isSelected) Color.White else textColor
                                )
                            }
                        }
                    }
                    if (rowMoods.size < columns) {
                        repeat(columns - rowMoods.size) {
                            Box(modifier = Modifier.weight(1f))
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
                            mainViewModel.addMoodEntry(it.name, iconId, it.value) { isUpdate ->
                                coroutineScope.launch {
                                    val msg = if (isUpdate) "Mood updated for today ✦" else "Mood logged ✦"
                                    snackbarHostState.showSnackbar(
                                        message = msg,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)
                ) {
                    Text(
                        text = if (todayMood == null) "Log My Mood" else "Update My Mood", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier
                    .padding(12.dp)
                    .border(1.dp, orange.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                containerColor = cardBg,
                contentColor = textColor
            ) {
                Text(text = data.visuals.message)
            }
        }
    }
}

@Composable
fun MoodAnalyticsCard(stats: MoodStats, orange: Color, cardBg: Color, textColor: Color, textMuted: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                        color = textMuted
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
                        color = textMuted
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
                trackColor = textColor.copy(alpha = 0.1f)
            )
        }
    }
}

data class MoodData(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val value: Int)
