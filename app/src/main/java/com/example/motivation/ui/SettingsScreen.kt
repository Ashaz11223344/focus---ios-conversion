package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val count by settingsViewModel.notificationCountPerDay.collectAsState()
    val streakReminderEnabled by settingsViewModel.streakReminderEnabled.collectAsState()
    val quietHoursEnabled by settingsViewModel.quietHoursEnabled.collectAsState()
    val quietHoursStart by settingsViewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by settingsViewModel.quietHoursEnd.collectAsState()
    
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // --- Main Notification Settings ---
        item {
            Text(
                "Notifications", 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BoxDefaults.cardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quotes per day", 
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "$count", 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = count.toFloat(), 
                        onValueChange = { settingsViewModel.setDailyCountSettings(it.roundToInt()) }, 
                        valueRange = 1f..10f, 
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                    )
                    Text(
                        "How many inspirational quotes you want to receive daily.", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // --- Quiet Hours Section ---
        item {
            Text(
                "Quiet Hours", 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BoxDefaults.cardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(label = "Enable Quiet Hours") {
                        Switch(
                            checked = quietHoursEnabled, 
                            onCheckedChange = { settingsViewModel.setQuietHoursEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                    Text(
                        "No notifications will be sent during this period.", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    
                    if (quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TimeSetting(
                                label = "Starts at",
                                time = formatMinutesToTime(quietHoursStart),
                                onClick = {
                                    showTimePicker(context, quietHoursStart) { h, m ->
                                        settingsViewModel.setQuietHoursTime(h * 60 + m, quietHoursEnd)
                                    }
                                }
                            )
                            TimeSetting(
                                label = "Ends at",
                                time = formatMinutesToTime(quietHoursEnd),
                                onClick = {
                                    showTimePicker(context, quietHoursEnd) { h, m ->
                                        settingsViewModel.setQuietHoursTime(quietHoursStart, h * 60 + m)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Streak Reminder Settings ---
        item {
            Text(
                "Reminders", 
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BoxDefaults.cardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingRow(label = "Streak reminder") {
                        Switch(
                            checked = streakReminderEnabled, 
                            onCheckedChange = { settingsViewModel.setStreakReminderEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                    Text(
                        "Get a reminder at 8 PM if you haven't checked in today.", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun TimeSetting(label: String, time: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        content()
    }
}

private fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val ampm = if (hours >= 12) "PM" else "AM"
    val displayHours = when {
        hours == 0 -> 12
        hours > 12 -> hours - 12
        else -> hours
    }
    return String.format("%d:%02d %s", displayHours, mins, ampm)
}

private fun showTimePicker(context: android.content.Context, currentMinutes: Int, onTimeSelected: (Int, Int) -> Unit) {
    val h = currentMinutes / 60
    val m = currentMinutes % 60
    TimePickerDialog(context, { _, hour, minute ->
        onTimeSelected(hour, minute)
    }, h, m, false).show()
}
