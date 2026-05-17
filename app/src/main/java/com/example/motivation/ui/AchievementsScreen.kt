package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AchievementsScreen(onNavigateHome: () -> Unit) {
    val achievements = listOf(
        AchievementItem("First Spark", "Viewed your first quote", true),
        AchievementItem("Consistency King", "Maintain a 7-day streak", false),
        AchievementItem("Deep Thinker", "Save 10 favorites", true),
        AchievementItem("Searcher", "Use the search feature", true),
        AchievementItem("Journalist", "Write 5 journal entries", false),
        AchievementItem("Motivation Master", "View 100 quotes", false)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Achievements",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(achievements) { achievement ->
                AchievementCard(achievement)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Back to Home")
        }
    }
}

data class AchievementItem(val title: String, val description: String, val isUnlocked: Boolean)

@Composable
fun AchievementCard(achievement: AchievementItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (achievement.isUnlocked) Icons.Default.Star else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray
                )
            }
        }
    }
}
