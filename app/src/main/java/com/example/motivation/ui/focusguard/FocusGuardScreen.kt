package com.example.motivation.ui.focusguard

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.motivation.R
import com.example.motivation.data.local.AppBlockRuleEntity
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.InterFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.viewmodel.FocusGuardViewModel
import com.example.motivation.ui.BoxDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusGuardScreen(
    navController: NavController,
    initialTab: Int = 0,
    viewModel: FocusGuardViewModel = viewModel()
) {
    val appBlockRules by viewModel.appBlockRules.collectAsState()

    val context = LocalContext.current
    var isUsageGranted by remember { mutableStateOf(viewModel.isUsageStatsGranted()) }
    var isOverlayGranted by remember { mutableStateOf(viewModel.isOverlayPermissionGranted()) }

    // Recheck permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isUsageGranted = viewModel.isUsageStatsGranted()
                isOverlayGranted = viewModel.isOverlayPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.fg_app_blocker),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isConsentGranted by viewModel.appBlockerConsentGranted.collectAsState()
            AppBlockerTab(
                isConsentGranted = isConsentGranted,
                isPermissionGranted = isUsageGranted,
                isOverlayPermissionGranted = isOverlayGranted,
                rules = appBlockRules,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBlockerTab(
    isConsentGranted: Boolean,
    isPermissionGranted: Boolean,
    isOverlayPermissionGranted: Boolean,
    rules: List<AppBlockRuleEntity>,
    viewModel: FocusGuardViewModel
) {
    var showConsentDialog by remember { mutableStateOf(false) }
    var showExplanationSheet by remember { mutableStateOf(false) }
    var showOverlayExplanationSheet by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var selectedPackageForBlock by remember { mutableStateOf<String?>(null) }
    var selectedAppNameForBlock by remember { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<AppBlockRuleEntity?>(null) }
    val context = LocalContext.current

    if (!isConsentGranted) {
        PermissionRequestCard(
            title = stringResource(id = R.string.fg_consent_needed_title),
            description = stringResource(id = R.string.fg_consent_needed_desc),
            onGrantClick = { showConsentDialog = true }
        )
    } else if (!isPermissionGranted) {
        PermissionRequestCard(
            title = stringResource(id = R.string.fg_usage_needed_title),
            description = stringResource(id = R.string.fg_usage_needed_desc),
            onGrantClick = { showExplanationSheet = true }
        )
    } else if (!isOverlayPermissionGranted) {
        PermissionRequestCard(
            title = stringResource(id = R.string.fg_overlay_needed_title),
            description = stringResource(id = R.string.fg_overlay_needed_desc),
            onGrantClick = { showOverlayExplanationSheet = true }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(id = R.string.fg_no_rules),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontFamily = LiterataFontFamily,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules) { rule ->
                        AppBlockRuleItem(
                            rule = rule,
                            onToggle = { viewModel.toggleAppBlockRule(rule) },
                            onDelete = { viewModel.deleteAppBlockRule(rule.packageName) },
                            onClick = { editTarget = rule }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAppPicker = true },
                containerColor = VibrantOrange,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add App Block")
            }
        }
    }

    if (showExplanationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExplanationSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(id = R.string.fg_permission_needed_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    stringResource(id = R.string.fg_usage_permission_body),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        showExplanationSheet = false
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.fg_open_settings), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showOverlayExplanationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOverlayExplanationSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(id = R.string.fg_permission_needed_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    stringResource(id = R.string.fg_overlay_permission_body),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        showOverlayExplanationSheet = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.fg_open_settings), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onAppSelected = { pkg, name ->
                selectedPackageForBlock = pkg
                selectedAppNameForBlock = name
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (selectedPackageForBlock != null && selectedAppNameForBlock != null) {
        AddAppBlockDialog(
            packageName = selectedPackageForBlock!!,
            appName = selectedAppNameForBlock!!,
            onSave = {
                viewModel.addAppBlockRule(it)
                selectedPackageForBlock = null
                selectedAppNameForBlock = null
            },
            onDismiss = {
                selectedPackageForBlock = null
                selectedAppNameForBlock = null
            }
        )
    }

    if (editTarget != null) {
        AddAppBlockDialog(
            packageName = editTarget!!.packageName,
            appName = editTarget!!.appName,
            existing = editTarget,
            onSave = {
                viewModel.addAppBlockRule(it)
                editTarget = null
            },
            onDismiss = { editTarget = null }
        )
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.fg_consent_title),
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.fg_consent_body),
                    fontFamily = InterFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAppBlockerConsentGranted(true)
                        showConsentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
                ) {
                    Text(
                        text = stringResource(id = R.string.fg_consent_agree),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConsentDialog = false }
                ) {
                    Text(
                        text = stringResource(id = R.string.fg_consent_disagree),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun PermissionRequestCard(
    title: String,
    description: String,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(id = R.string.fg_grant_access),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
fun AppBlockRuleItem(
    rule: AppBlockRuleEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(rule.packageName) {
        try {
            appIcon = context.packageManager.getApplicationIcon(rule.packageName)
        } catch (e: Exception) {
            // Ignore
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (appIcon != null) {
                        val bitmap = remember(appIcon) { appIcon!!.toBitmap(width = 96, height = 96) }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = rule.appName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            rule.appName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%02d:%02d – %02d:%02d", rule.startHour, rule.startMinute, rule.endHour, rule.endMinute),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VibrantOrange,
                            checkedTrackColor = VibrantOrange.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DaysDisplayRow(daysOfWeek = rule.daysOfWeek)
        }
    }
}

@Composable
fun DaysDisplayRow(daysOfWeek: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        days.forEachIndexed { index, day ->
            val isSelected = (daysOfWeek and (1 shl index)) != 0
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) VibrantOrange else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
