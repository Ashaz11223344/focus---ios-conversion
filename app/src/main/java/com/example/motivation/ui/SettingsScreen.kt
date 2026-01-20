package com.example.motivation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val contentType by settingsViewModel.notificationContentType.collectAsState()
    val notificationMode by settingsViewModel.notificationMode.collectAsState()
    val interval by settingsViewModel.notificationIntervalMinutes.collectAsState()
    val count by settingsViewModel.notificationCountPerDay.collectAsState()
    val streakReminderEnabled by settingsViewModel.streakReminderEnabled.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp)) }

        // --- Main Notification Settings ---
        item {
            Text("Main Notifications", style = MaterialTheme.typography.titleLarge)
            // Content Type Selection
            Text("Content Type", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = contentType == "Affirmation", onClick = { settingsViewModel.setNotificationContentType("Affirmation") })
                    Text("Affirmation")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = contentType == "Quote", onClick = { settingsViewModel.setNotificationContentType("Quote") })
                    Text("Quote")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timing Mode Selection
            Text("Timing Mode", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = notificationMode == "DailyCount", onClick = { settingsViewModel.setNotificationMode("DailyCount") })
                    Text("Per Day")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = notificationMode == "Frequency", onClick = { settingsViewModel.setNotificationMode("Frequency") })
                    Text("Frequency")
                }
            }

            AnimatedVisibility(visible = notificationMode == "DailyCount") {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text("Notifications per day: $count", style = MaterialTheme.typography.titleSmall)
                    Slider(value = count.toFloat(), onValueChange = { settingsViewModel.setDailyCountSettings(it.roundToInt()) }, valueRange = 1f..10f, steps = 8)
                }
            }

            AnimatedVisibility(visible = notificationMode == "Frequency") {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text("Notify every: $interval minutes", style = MaterialTheme.typography.titleSmall)
                    Slider(value = interval.toFloat(), onValueChange = { settingsViewModel.setFrequencySettings(it.roundToInt()) }, valueRange = 1f..120f, steps = 118)
                    Text(text = "Note: The minimum interval is 15 minutes.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Divider(modifier = Modifier.padding(vertical = 24.dp)) }

        // --- Streak Reminder Settings ---
        item {
            Text("Streak Reminder", style = MaterialTheme.typography.titleLarge)
            SettingRow(label = "Enable end-of-day reminder") {
                Switch(checked = streakReminderEnabled, onCheckedChange = { settingsViewModel.setStreakReminderEnabled(it) })
            }
            Text("If you haven\'t completed your intent, you\'ll get a reminder around 8 PM.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        content()
    }
}
