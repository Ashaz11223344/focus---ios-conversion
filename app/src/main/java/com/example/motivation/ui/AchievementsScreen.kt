package com.example.motivation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.motivation.viewmodel.ProfileViewModel
import com.example.motivation.data.local.Achievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateHome: () -> Unit
) {
    val achievements by profileViewModel.achievements.collectAsState()
    val streakCount by profileViewModel.streakCount.collectAsState()

    val unlockedCount = remember(achievements) {
        achievements.count { it.isUnlocked }
    }
    val totalCount = achievements.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Achievements",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Progress Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(VibrantOrange.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_achievements),
                            contentDescription = null,
                            tint = VibrantOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Milestones Unlocked",
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$unlockedCount of $totalCount completed • Streak: $streakCount days",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Achievements Grid
            if (totalCount == 0) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibrantOrange)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(achievements, key = { it.achievementId }) { achievement ->
                        AchievementGridCard(achievement = achievement)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementGridCard(achievement: Achievement) {
    val progressRatio = remember(achievement.progressCurrent, achievement.progressTarget) {
        if (achievement.progressTarget > 0) {
            (achievement.progressCurrent.toFloat() / achievement.progressTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    val progress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(1000),
        label = "progressAnim"
    )

    val unlockedDateFormatted = remember(achievement.unlockedDate) {
        achievement.unlockedDate?.let {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(it))
        } ?: ""
    }

    val cardBg = if (achievement.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (achievement.isUnlocked) {
        androidx.compose.foundation.BorderStroke(1.dp, VibrantOrange.copy(0.4f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
    val titleColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f)
    val descColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
    val footColor = if (achievement.isUnlocked) VibrantOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
    val progressArcTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(86.dp)
            ) {
                // Progress circle around the badge for locked ones
                if (!achievement.isUnlocked) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = progressArcTrackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawArc(
                            color = VibrantOrange.copy(alpha = 0.6f),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                
                BadgeIcon(
                    icon = getAchievementIcon(achievement.achievementId),
                    tier = achievement.tier,
                    size = 72.dp,
                    isUnlocked = achievement.isUnlocked
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = achievement.title,
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = titleColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = achievement.description,
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                color = descColor,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (achievement.isUnlocked) "Unlocked $unlockedDateFormatted" else "Progress: ${achievement.progressCurrent}/${achievement.progressTarget}",
                fontFamily = InterFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = footColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
