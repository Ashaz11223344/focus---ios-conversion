@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.motivation.ui

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.motivation.R
import com.example.motivation.model.QuoteCategory
import com.example.motivation.receiver.CategoryRotationEngine
import com.example.motivation.ui.theme.EpilogueFontFamily
import com.example.motivation.viewmodel.BackupState
import com.example.motivation.viewmodel.ProfileViewModel
import com.example.motivation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val EmberOrange = Color(0xFFFF6D2E)
private val InkSurface = Color(0xFF131313)
private val InkSurfaceBright = Color(0xFF393939)
private val InkBackground = Color(0xFF0A0A0A)

@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val userName by profileViewModel.userName.collectAsState()
    val userProfilePhotoUri by profileViewModel.userProfilePhotoUri.collectAsState()
    val enableBadgeDisplay by profileViewModel.enableBadgeDisplay.collectAsState()
    val profileCreatedDate by profileViewModel.profileCreatedDate.collectAsState()
    val achievements by profileViewModel.achievements.collectAsState()

    val count by settingsViewModel.notificationCountPerDay.collectAsState()
    val streakReminderEnabled by settingsViewModel.streakReminderEnabled.collectAsState()
    val quietHoursEnabled by settingsViewModel.quietHoursEnabled.collectAsState()
    val quietHoursStart by settingsViewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by settingsViewModel.quietHoursEnd.collectAsState()
    val moodReminderEnabled by settingsViewModel.moodReminderEnabled.collectAsState()
    val moodReminderTime by settingsViewModel.moodReminderTime.collectAsState()
    val quoteNotificationsEnabled by settingsViewModel.quoteNotificationsEnabled.collectAsState()
    val quoteScheduleType by settingsViewModel.quoteScheduleType.collectAsState()
    val quoteTimeSlot1 by settingsViewModel.quoteTimeSlot1.collectAsState()
    val quoteTimeSlot2 by settingsViewModel.quoteTimeSlot2.collectAsState()
    val quoteTimeSlot3 by settingsViewModel.quoteTimeSlot3.collectAsState()
    val selectedGenreCategories by settingsViewModel.selectedGenreCategories.collectAsState()
    val todayActiveCategories by settingsViewModel.todayActiveCategories.collectAsState()
    val showCategoryPicker by settingsViewModel.showCategoryPicker.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isReminderInQuietHours = remember(moodReminderTime, quietHoursEnabled, quietHoursStart, quietHoursEnd) {
        if (quietHoursEnabled) {
            if (quietHoursStart < quietHoursEnd) {
                moodReminderTime in quietHoursStart..quietHoursEnd
            } else {
                moodReminderTime >= quietHoursStart || moodReminderTime <= quietHoursEnd
            }
        } else {
            false
        }
    }

    val exactAlarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            settingsViewModel.setMoodReminderEnabled(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Notifications permission is required to receive daily mood reminders.")
            }
        }
    }

    // --- Backup & Restore State & Launchers ---
    val backupState by settingsViewModel.backupState.collectAsState()

    var showBackupRestoreBottomSheet by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }

    var tempExportPassword by remember { mutableStateOf("") }
    var tempExportConfirmPassword by remember { mutableStateOf("") }
    var tempImportPassword by remember { mutableStateOf("") }

    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var selectedExportUri by remember { mutableStateOf<Uri?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            selectedExportUri = uri
            settingsViewModel.exportData(uri, tempExportPassword)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedImportUri = uri
            showImportPasswordDialog = true
        }
    }

    val prefs = remember(context) { context.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE) }
    var quickWallpaperOnHold by remember { 
        mutableStateOf(prefs.getBoolean("quick_wallpaper_on_hold", true)) 
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- Custom Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    fontFamily = EpilogueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // --- PROFILE & ACHIEVEMENTS ---
                item {
                    val joinedDateFormatted = remember(profileCreatedDate) {
                        if (profileCreatedDate > 0L) {
                            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            sdf.format(Date(profileCreatedDate))
                        } else {
                            "Not joined"
                        }
                    }
                    val unlockedBadgesCount = remember(achievements) {
                        achievements.count { it.isUnlocked }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PROFILE & ACHIEVEMENTS",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        SettingsCard {
                            // Avatar & Name Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate("my_profile") }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(56.dp)) {
                                    ProfileAvatar(
                                        photoUriString = userProfilePhotoUri,
                                        userName = userName,
                                        size = 56.dp
                                    )
                                    if (enableBadgeDisplay) {
                                        val highestBadge = getHighestTierUnlockedBadge(achievements)
                                        if (highestBadge != null) {
                                            BadgeIcon(
                                                icon = getAchievementIcon(highestBadge.achievementId),
                                                tier = highestBadge.tier,
                                                size = 20.dp,
                                                isUnlocked = true,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(x = 2.dp, y = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = userName.ifBlank { "Focus User" },
                                            fontFamily = EpilogueFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit Profile",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Joined: $joinedDateFormatted",
                                        fontFamily = EpilogueFontFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // My Profile
                            SettingsItem(
                                icon = Icons.Rounded.Person,
                                title = "My Profile",
                                subtitle = "Edit your name and profile picture",
                                onClick = { navController.navigate("my_profile") }
                            )

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // View Achievements
                            SettingsItem(
                                icon = Icons.Rounded.EmojiEvents,
                                title = "View Achievements",
                                subtitle = "See locked & unlocked achievement badges ($unlockedBadgesCount unlocked)",
                                onClick = { navController.navigate("achievements") }
                            )

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // Show Badge on Home
                            SettingsItem(
                                icon = Icons.Rounded.Badge,
                                title = "Show Badge on Home",
                                subtitle = "Display your rank on the widget",
                                action = {
                                    Switch(
                                        checked = enableBadgeDisplay,
                                        onCheckedChange = { profileViewModel.setEnableBadgeDisplay(it) },
                                        colors = EmberSwitchColors()
                                    )
                                }
                            )
                        }
                    }
                }

                // --- QUOTE NOTIFICATIONS ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "QUOTE NOTIFICATIONS",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        SettingsCard {
                            SettingsItem(
                                icon = Icons.Rounded.Notifications,
                                title = "Quote Notifications",
                                subtitle = "Receive daily inspirational quotes to stay motivated.",
                                action = {
                                    Switch(
                                        checked = quoteNotificationsEnabled,
                                        onCheckedChange = { settingsViewModel.setQuoteNotificationsEnabled(it) },
                                        colors = EmberSwitchColors()
                                    )
                                }
                            )

                            AnimatedVisibility(
                                visible = quoteNotificationsEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Divider(color = Color.Black, thickness = 1.dp)
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Quotes per day",
                                                fontFamily = EpilogueFontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$count",
                                                fontFamily = EpilogueFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = EmberOrange
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Slider(
                                            value = count.toFloat(),
                                            onValueChange = { settingsViewModel.setDailyCountSettings(it.roundToInt()) },
                                            valueRange = 1f..3f,
                                            steps = 1,
                                            colors = SliderDefaults.colors(
                                                thumbColor = EmberOrange,
                                                activeTrackColor = EmberOrange,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                        Text(
                                            text = "How many inspirational quotes you want to receive daily (maximum 3).",
                                            fontFamily = EpilogueFontFamily,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                     Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                                     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                         Text(
                                             text = "Quote Schedule",
                                             fontFamily = EpilogueFontFamily,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 15.sp,
                                             color = MaterialTheme.colorScheme.onSurface
                                         )
                                         
                                         // Segmented Control (Theme-aware)
                                         Row(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp))
                                                 .border(
                                                     BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                     shape = RoundedCornerShape(10.dp)
                                                 )
                                                 .padding(4.dp),
                                             horizontalArrangement = Arrangement.spacedBy(4.dp)
                                         ) {
                                             listOf("Random", "Specific Time").forEach { option ->
                                                 val isSelected = if (option == "Random") quoteScheduleType == "Random" else quoteScheduleType == "Specific"
                                                 Box(
                                                     modifier = Modifier
                                                         .weight(1f)
                                                         .clip(RoundedCornerShape(8.dp))
                                                         .background(
                                                             color = if (isSelected) EmberOrange.copy(alpha = 0.2f) else Color.Transparent
                                                         )
                                                         .border(
                                                             border = if (isSelected) BorderStroke(1.dp, EmberOrange.copy(alpha = 0.6f)) else BorderStroke(0.dp, Color.Transparent),
                                                             shape = RoundedCornerShape(8.dp)
                                                         )
                                                         .clickable {
                                                             val newType = if (option == "Random") "Random" else "Specific"
                                                             settingsViewModel.setQuoteScheduleType(newType)
                                                         }
                                                         .padding(vertical = 12.dp),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Text(
                                                         text = option.uppercase(),
                                                         fontFamily = EpilogueFontFamily,
                                                         fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                         fontSize = 12.sp,
                                                         letterSpacing = 0.5.sp,
                                                         color = if (isSelected) EmberOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                     )
                                                 }
                                             }
                                         }

                                         AnimatedVisibility(
                                             visible = quoteScheduleType == "Specific",
                                             enter = expandVertically() + fadeIn(),
                                             exit = shrinkVertically() + fadeOut()
                                         ) {
                                             Column(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .padding(top = 12.dp)
                                                     .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                                                     .padding(horizontal = 16.dp, vertical = 8.dp),
                                                 verticalArrangement = Arrangement.spacedBy(8.dp)
                                             ) {
                                                 for (i in 1..count) {
                                                     val slotTime = when (i) {
                                                         1 -> quoteTimeSlot1
                                                         2 -> quoteTimeSlot2
                                                         3 -> quoteTimeSlot3
                                                         else -> -1
                                                     }
                                                     val isRandom = slotTime == -1
                                                     val timeText = if (isRandom) "" else formatMinutesToTime(slotTime)

                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .height(48.dp),
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         horizontalArrangement = Arrangement.SpaceBetween
                                                     ) {
                                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                                             Icon(
                                                                 imageVector = Icons.Rounded.AccessTime,
                                                                 contentDescription = null,
                                                                 tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                                 modifier = Modifier.size(20.dp)
                                                             )
                                                             Spacer(modifier = Modifier.width(12.dp))
                                                             Text(
                                                                 text = "Quote $i",
                                                                 fontFamily = EpilogueFontFamily,
                                                                 fontWeight = FontWeight.Medium,
                                                                 fontSize = 14.sp,
                                                                 color = MaterialTheme.colorScheme.onSurface
                                                             )
                                                         }

                                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                                             if (isRandom) {
                                                                 Text(
                                                                     text = "+ Add Time",
                                                                     fontFamily = EpilogueFontFamily,
                                                                     fontWeight = FontWeight.Bold,
                                                                     fontSize = 14.sp,
                                                                     color = EmberOrange,
                                                                     modifier = Modifier
                                                                         .clickable {
                                                                             showTimePicker(context, 480) { h, m ->
                                                                                 validateAndSetTimeSlot(
                                                                                     i, h * 60 + m, count,
                                                                                     quoteTimeSlot1, quoteTimeSlot2, quoteTimeSlot3,
                                                                                     quietHoursEnabled, quietHoursStart, quietHoursEnd,
                                                                                     settingsViewModel, scope, snackbarHostState
                                                                                 )
                                                                             }
                                                                         }
                                                                         .padding(8.dp)
                                                                 )
                                                             } else {
                                                                 Text(
                                                                     text = timeText,
                                                                     fontFamily = EpilogueFontFamily,
                                                                     fontSize = 14.sp,
                                                                     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                                     modifier = Modifier.padding(end = 8.dp)
                                                                 )
                                                                 Text(
                                                                     text = "Edit",
                                                                     fontFamily = EpilogueFontFamily,
                                                                     fontWeight = FontWeight.Bold,
                                                                     fontSize = 14.sp,
                                                                     color = EmberOrange,
                                                                     modifier = Modifier
                                                                         .clickable {
                                                                             showTimePicker(context, slotTime) { h, m ->
                                                                                 validateAndSetTimeSlot(
                                                                                     i, h * 60 + m, count,
                                                                                     quoteTimeSlot1, quoteTimeSlot2, quoteTimeSlot3,
                                                                                     quietHoursEnabled, quietHoursStart, quietHoursEnd,
                                                                                     settingsViewModel, scope, snackbarHostState
                                                                                 )
                                                                             }
                                                                         }
                                                                         .padding(8.dp)
                                                                 )
                                                             }
                                                         }
                                                     }
                                                     if (i < count) {
                                                         Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                }
                            }
                        }
                    }
                }

                // --- APP EXPERIENCE ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "APP EXPERIENCE",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        SettingsCard {
                            // Quiet Hours
                            val quietHoursSubtext = if (quietHoursEnabled) {
                                "No alerts from ${formatMinutesToTime(quietHoursStart)} to ${formatMinutesToTime(quietHoursEnd)}"
                            } else {
                                "Quiet Hours is currently disabled"
                            }
                            SettingsItem(
                                icon = Icons.Rounded.NightsStay,
                                title = "Quiet Hours",
                                subtitle = quietHoursSubtext,
                                action = {
                                    Switch(
                                        checked = quietHoursEnabled,
                                        onCheckedChange = { settingsViewModel.setQuietHoursEnabled(it) },
                                        colors = EmberSwitchColors()
                                    )
                                },
                                onClick = if (quietHoursEnabled) {
                                    {
                                        showTimePicker(context, quietHoursStart) { h1, m1 ->
                                            showTimePicker(context, quietHoursEnd) { h2, m2 ->
                                                settingsViewModel.setQuietHoursTime(h1 * 60 + m1, h2 * 60 + m2)
                                            }
                                        }
                                    }
                                } else null
                            )
                            
                            if (quietHoursEnabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // Daily Mood Reminder
                            val moodReminderSubtext = if (moodReminderEnabled) {
                                "Check-in at ${formatMinutesToTime(moodReminderTime)}"
                            } else {
                                "Mood reminder is currently disabled"
                            }
                            SettingsItem(
                                icon = Icons.Rounded.Psychology,
                                title = "Daily Mood Reminder",
                                subtitle = moodReminderSubtext,
                                action = {
                                    Switch(
                                        checked = moodReminderEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    settingsViewModel.setMoodReminderEnabled(true)
                                                }
                                            } else {
                                                settingsViewModel.setMoodReminderEnabled(false)
                                            }
                                        },
                                        colors = EmberSwitchColors()
                                    )
                                },
                                onClick = if (moodReminderEnabled) {
                                    {
                                        showTimePicker(context, moodReminderTime) { h, m ->
                                            settingsViewModel.setMoodReminderTime(h * 60 + m)
                                        }
                                    }
                                } else null
                            )
                            if (moodReminderEnabled && isReminderInQuietHours) {
                                Text(
                                    text = "⚠️ Falls inside Quiet Hours. Scheduled for after quiet hours end.",
                                    fontFamily = EpilogueFontFamily,
                                    fontSize = 12.sp,
                                    color = EmberOrange,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // Quick Wallpaper on Hold
                            SettingsItem(
                                icon = Icons.Rounded.Wallpaper,
                                title = "Quick Wallpaper on Hold",
                                subtitle = "Long press any quote to open designer",
                                action = {
                                    Switch(
                                        checked = quickWallpaperOnHold,
                                        onCheckedChange = { checked ->
                                            quickWallpaperOnHold = checked
                                            prefs.edit().putBoolean("quick_wallpaper_on_hold", checked).apply()
                                        },
                                        colors = EmberSwitchColors()
                                    )
                                }
                            )

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // Category Picker trigger (Notification Categories)
                            SettingsItem(
                                icon = Icons.Rounded.Tune,
                                title = "Notification Categories",
                                subtitle = when {
                                    selectedGenreCategories.isEmpty() -> "All categories (no filter)"
                                    selectedGenreCategories.size == 1 -> "1 category selected"
                                    else -> "${selectedGenreCategories.size} categories selected"
                                },
                                onClick = { settingsViewModel.openCategoryPicker() }
                            )

                            if (selectedGenreCategories.isNotEmpty() && selectedGenreCategories.size > count) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 56.dp)
                                ) {
                                    Text(
                                        text = "Today's Active Categories",
                                        fontFamily = EpilogueFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        todayActiveCategories.forEach { activeCat ->
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = activeCat.displayName,
                                                        fontFamily = EpilogueFontFamily,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF121212)
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = activeCat.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = Color(0xFF121212)
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = EmberOrange,
                                                    labelColor = Color(0xFF121212)
                                                ),
                                                border = null,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val cycleDays = CategoryRotationEngine.fullCycleDays(selectedGenreCategories.size, count)
                                    Text(
                                        text = "Rotates daily · Full cycle in $cycleDays days",
                                        fontFamily = EpilogueFontFamily,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }

                // --- SECURITY & FOCUS ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SECURITY & FOCUS",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        SettingsCard {
                            // Focus Guard (App Blocker)
                            SettingsItem(
                                icon = Icons.Rounded.Shield,
                                title = "App Blocker (Focus Guard)",
                                subtitle = "Prevent distraction during sessions",
                                onClick = { navController.navigate("focus_guard/0") }
                            )

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // Backup & Restore
                            SettingsItem(
                                icon = Icons.Rounded.Backup,
                                title = "Backup & Restore",
                                subtitle = "Local data pack/export and import functionality",
                                action = {
                                    Icon(
                                        imageVector = Icons.Rounded.Autorenew,
                                        contentDescription = "Sync",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { showBackupRestoreBottomSheet = true }
                                    )
                                },
                                onClick = { showBackupRestoreBottomSheet = true }
                            )
                        }
                    }
                }

                // --- THEME MODE ---
                item {
                    val activeThemeMode by settingsViewModel.themeMode.collectAsState()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "THEME MODE",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val paddingPx = with(density) { 4.dp.toPx() }
                        var rowWidthPx by remember { mutableStateOf(1) }
                        val targetIndex = when (activeThemeMode) {
                            com.example.motivation.ui.theme.ThemeMode.SYSTEM -> 0
                            com.example.motivation.ui.theme.ThemeMode.LIGHT -> 1
                            com.example.motivation.ui.theme.ThemeMode.DARK -> 2
                        }
                        var isDragging by remember { mutableStateOf(false) }
                        var dragFraction by remember { mutableStateOf(targetIndex.toFloat()) }

                        // Sync dragFraction to targetIndex when not dragging
                        LaunchedEffect(targetIndex, isDragging) {
                            if (!isDragging) {
                                dragFraction = targetIndex.toFloat()
                            }
                        }

                        val animFraction by animateFloatAsState(
                            targetValue = if (isDragging) dragFraction else targetIndex.toFloat(),
                            animationSpec = if (isDragging) snap() else spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "themeFraction"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .onSizeChanged { rowWidthPx = it.width }
                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(99.dp))
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(99.dp)
                                )
                                .padding(4.dp)
                                .pointerInput(rowWidthPx) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            isDragging = true
                                            val fraction = (offset.x / rowWidthPx) * 3f - 0.5f
                                            dragFraction = fraction.coerceIn(0f, 2f)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val delta = (dragAmount.x / rowWidthPx) * 3f
                                            dragFraction = (dragFraction + delta).coerceIn(0f, 2f)
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            val nearestIndex = dragFraction.roundToInt().coerceIn(0, 2)
                                            val newMode = when (nearestIndex) {
                                                0 -> com.example.motivation.ui.theme.ThemeMode.SYSTEM
                                                1 -> com.example.motivation.ui.theme.ThemeMode.LIGHT
                                                else -> com.example.motivation.ui.theme.ThemeMode.DARK
                                            }
                                            settingsViewModel.setThemeMode(newMode)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Sliding background pill
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(1f / 3f)
                                    .offset {
                                        val totalAvailableWidth = rowWidthPx - (paddingPx * 2)
                                        val xOffset = (animFraction * totalAvailableWidth / 3f).roundToInt()
                                        IntOffset(x = xOffset, y = 0)
                                    }
                                    .background(Color(0xFFFC6E20).copy(alpha = 0.15f), shape = RoundedCornerShape(99.dp))
                            )

                            // Interactive options Row
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val modes = listOf(
                                    com.example.motivation.ui.theme.ThemeMode.SYSTEM to "System",
                                    com.example.motivation.ui.theme.ThemeMode.LIGHT to "Light",
                                    com.example.motivation.ui.theme.ThemeMode.DARK to "Dark"
                                )
                                modes.forEach { (mode, label) ->
                                    val isSelected = activeThemeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable {
                                                settingsViewModel.setThemeMode(mode)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = EpilogueFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFFFC6E20) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SUPPORT ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SUPPORT",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        SettingsCard {
                            var showRestartConfirmation by remember { mutableStateOf(false) }

                            if (showRestartConfirmation) {
                                AlertDialog(
                                    onDismissRequest = { showRestartConfirmation = false },
                                    title = {
                                        Text(
                                            text = "Restart the guide?",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontFamily = EpilogueFontFamily
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = "This will replay the welcome walkthrough from the beginning.",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            fontFamily = EpilogueFontFamily
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showRestartConfirmation = false
                                                val sharedPrefs = context.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
                                                sharedPrefs.edit().putBoolean("onboarding_completed", false).apply()

                                                val intent = Intent(context, com.example.motivation.ui.onboarding.OnboardingActivity::class.java).apply {
                                                    putExtra("is_restart", true)
                                                }
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmberOrange,
                                                contentColor = Color(0xFF121212)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Restart", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { showRestartConfirmation = false }
                                        ) {
                                            Text("Cancel", color = Color.White.copy(alpha = 0.6f), fontFamily = EpilogueFontFamily)
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Restart Guide
                            SettingsItem(
                                icon = Icons.Rounded.PlayArrow,
                                title = "Restart Guide",
                                subtitle = "Replay the welcome walkthrough",
                                onClick = { showRestartConfirmation = true }
                            )

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

                            // Privacy Policy
                            SettingsItem(
                                icon = Icons.Rounded.Description,
                                title = "Privacy Policy",
                                subtitle = "View our privacy terms and data safety information",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.getfocus.online/privacy"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Category Picker Sheet ---
    if (showCategoryPicker) {
        CategoryPickerBottomSheet(
            settingsViewModel = settingsViewModel,
            onDismiss = { settingsViewModel.closeCategoryPicker() }
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )

    // --- Backup & Restore Sheet ---
    if (showBackupRestoreBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBackupRestoreBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Backup & Restore",
                    fontFamily = EpilogueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Secure your profile, notes, achievements and custom settings.",
                    fontFamily = EpilogueFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Export Data Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showBackupRestoreBottomSheet = false
                            tempExportPassword = ""
                            tempExportConfirmPassword = ""
                            selectedExportUri = null
                            selectedImportUri = null
                            showExportPasswordDialog = true
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(EmberOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Backup,
                            contentDescription = null,
                            tint = EmberOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Data",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Encrypt and export your data to a local file.",
                            fontFamily = EpilogueFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Import Data Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showBackupRestoreBottomSheet = false
                            tempImportPassword = ""
                            selectedExportUri = null
                            selectedImportUri = null
                            openDocumentLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(EmberOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsBackupRestore,
                            contentDescription = null,
                            tint = EmberOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Data",
                            fontFamily = EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Restore data from an existing backup file.",
                            fontFamily = EpilogueFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Export Password Dialog ---
    if (showExportPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = {
                Text("Create Backup Password", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set a password to encrypt your backup file. You will need this password to restore your data.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = EpilogueFontFamily),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMsg = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = EmberOrange)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = EmberOrange,
                            focusedLabelColor = EmberOrange,
                            unfocusedLabelColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            errorMsg = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = EmberOrange)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = EmberOrange,
                            focusedLabelColor = EmberOrange,
                            unfocusedLabelColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password.length < 4) {
                            errorMsg = "Password must be at least 4 characters."
                        } else if (password != confirmPassword) {
                            errorMsg = "Passwords do not match."
                        } else {
                            tempExportPassword = password
                            showExportPasswordDialog = false
                            val dateString = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            createDocumentLauncher.launch("Focus_Backup_$dateString.focus")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmberOrange,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontFamily = EpilogueFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // --- Import Password Dialog ---
    if (showImportPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportPasswordDialog = false },
            title = {
                Text("Enter Backup Password", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Please enter the password you created when exporting this backup file to decrypt and restore your profile.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = EpilogueFontFamily),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMsg = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null, tint = EmberOrange)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = EmberOrange,
                            focusedLabelColor = EmberOrange,
                            unfocusedLabelColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password.isEmpty()) {
                            errorMsg = "Password cannot be empty."
                        } else {
                            tempImportPassword = password
                            showImportPasswordDialog = false
                            selectedImportUri?.let { uri ->
                                settingsViewModel.importData(uri, password)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmberOrange,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportPasswordDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontFamily = EpilogueFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // --- Backup & Restore State Overlays ---
    if (backupState != BackupState.Idle) {
        AlertDialog(
            onDismissRequest = {
                if (backupState !is BackupState.Loading) {
                    settingsViewModel.resetBackupState()
                }
            },
            confirmButton = {
                when (backupState) {
                    is BackupState.Success -> {
                        Button(
                            onClick = {
                                val isImportSuccess = (backupState as BackupState.Success).message.contains("Restored", ignoreCase = true)
                                settingsViewModel.resetBackupState()
                                if (isImportSuccess) {
                                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                    context.startActivity(intent)
                                    Runtime.getRuntime().exit(0)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberOrange,
                                contentColor = Color(0xFF121212)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily)
                        }
                    }
                    is BackupState.Failure -> {
                        Button(
                            onClick = { settingsViewModel.resetBackupState() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberOrange,
                                contentColor = Color(0xFF121212)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily)
                        }
                    }
                    else -> {}
                }
            },
            title = {
                val title = when (backupState) {
                    is BackupState.Loading -> "Processing..."
                    is BackupState.Success -> "Success!"
                    is BackupState.Failure -> "Operation Failed"
                    else -> ""
                }
                Text(title, fontWeight = FontWeight.Bold, fontFamily = EpilogueFontFamily, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    when (backupState) {
                        is BackupState.Loading -> {
                            CircularProgressIndicator(color = EmberOrange)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedExportUri != null) "Encrypting and exporting your data..." else "Decrypting and restoring your data...",
                                textAlign = TextAlign.Center,
                                fontFamily = EpilogueFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is BackupState.Success -> {
                            var triggerAnimation by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                triggerAnimation = true
                            }
                            val checkmarkScale by animateFloatAsState(
                                targetValue = if (triggerAnimation) 1f else 0.2f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "checkmarkScale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .scale(checkmarkScale)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f), shape = CircleShape)
                                    .border(2.dp, Color(0xFF2E7D32), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (backupState as BackupState.Success).message,
                                textAlign = TextAlign.Center,
                                fontFamily = EpilogueFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is BackupState.Failure -> {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .scale(pulseScale)
                                    .background(Color(0xFFC62828).copy(alpha = 0.15f), shape = CircleShape)
                                    .border(2.dp, Color(0xFFC62828), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (backupState as BackupState.Failure).reason,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontFamily = EpilogueFontFamily
                            )
                        }
                        else -> {}
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        )
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
            fontFamily = EpilogueFontFamily,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = time,
            fontFamily = EpilogueFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = EmberOrange
        )
    }
}

@Composable
fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(EmberOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmberOrange,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = EpilogueFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = EpilogueFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(8.dp))
            action()
        } else if (onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmberSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = EmberOrange,
    checkedTrackColor = EmberOrange.copy(alpha = 0.2f),
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
)

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

private fun showTimePicker(context: Context, currentMinutes: Int, onTimeSelected: (Int, Int) -> Unit) {
    val h = currentMinutes / 60
    val m = currentMinutes % 60
    TimePickerDialog(context, { _, hour, minute ->
        onTimeSelected(hour, minute)
    }, h, m, false).show()
}

private fun validateAndSetTimeSlot(
    slotIndex: Int,
    selectedTime: Int,
    count: Int,
    slot1: Int,
    slot2: Int,
    slot3: Int,
    quietHoursEnabled: Boolean,
    quietHoursStart: Int,
    quietHoursEnd: Int,
    settingsViewModel: SettingsViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    var hasDuplicate = false
    for (j in 1..count) {
        if (j != slotIndex) {
            val otherTime = when (j) {
                1 -> slot1
                2 -> slot2
                3 -> slot3
                else -> -1
            }
            if (otherTime == selectedTime) {
                hasDuplicate = true
            }
        }
    }
    
    if (hasDuplicate) {
        scope.launch {
            snackbarHostState.showSnackbar("⚠️ Two notifications are set to the same time")
        }
    }
    
    if (quietHoursEnabled) {
        val inQH = if (quietHoursStart < quietHoursEnd) {
            selectedTime in quietHoursStart..quietHoursEnd
        } else {
            selectedTime >= quietHoursStart || selectedTime <= quietHoursEnd
        }
        if (inQH) {
            scope.launch {
                snackbarHostState.showSnackbar("⚠️ This time falls within your Quiet Hours. Notification may be suppressed.")
            }
        }
    }
    
    settingsViewModel.setQuoteTimeSlot(slotIndex, selectedTime)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val initialSelected by settingsViewModel.selectedGenreCategories.collectAsState()
    val tempSelected = remember(initialSelected) { initialSelected.toMutableStateList() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = InkSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x33FFE7D0)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Choose Notification Categories",
                fontFamily = EpilogueFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your daily quotes will only come from these genres.",
                fontFamily = EpilogueFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(QuoteCategory.all.size) { index ->
                    val category = QuoteCategory.all[index]
                    val isSelected = tempSelected.contains(category.id)
                    CategoryChipCard(
                        category = category,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                tempSelected.remove(category.id)
                            } else {
                                tempSelected.add(category.id)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { tempSelected.clear() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2E2E2E))
                ) {
                    Text(
                        text = "Clear All",
                        fontFamily = EpilogueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        settingsViewModel.applySelectedCategories(tempSelected.toList())
                        settingsViewModel.closeCategoryPicker()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmberOrange,
                        contentColor = Color(0xFF121212)
                    )
                ) {
                    Text(
                        text = "Apply",
                        fontFamily = EpilogueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChipCard(
    category: QuoteCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val transitionSpec = tween<Color>(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )

    val targetBgColor = if (isSelected) {
        EmberOrange.copy(alpha = 0.15f)
    } else {
        Color(0xFF1E1E1E)
    }

    val targetBorderColor = if (isSelected) {
        EmberOrange
    } else {
        Color.Transparent
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = transitionSpec,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = transitionSpec,
        label = "borderColor"
    )

    val contentColor = if (isSelected) {
        EmberOrange
    } else {
        Color.White.copy(alpha = 0.8f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = category.displayName,
                    fontFamily = EpilogueFontFamily,
                    fontSize = 12.sp,
                    color = contentColor
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = EmberOrange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
