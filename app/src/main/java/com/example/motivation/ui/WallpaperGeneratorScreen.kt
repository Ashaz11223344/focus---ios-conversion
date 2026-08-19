package com.example.motivation.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.app.WallpaperManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlin.math.roundToInt
import com.example.motivation.util.WallpaperAlignment
import com.example.motivation.util.WallpaperFont
import com.example.motivation.util.WallpaperRenderer
import com.example.motivation.util.WallpaperTheme
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.InterFontFamily
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun WallpaperGeneratorScreen(
    navController: NavController,
    quoteText: String,
    author: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Force Portrait Orientation Constraint on Entrance
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 2. Editor Parameter States & Responsive Configurations
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompact = screenWidthDp < 360
    val isLarge = screenWidthDp > 420

    val previewWidthDp = screenWidthDp.dp * 0.85f
    val previewHeightDp = previewWidthDp * (16f / 9f)

    val density = LocalDensity.current
    val previewWidthPx = remember(screenWidthDp) {
        with(density) { previewWidthDp.toPx().toInt() }
    }
    val previewHeightPx = remember(screenWidthDp) {
        with(density) { previewHeightDp.toPx().toInt() }
    }

    var selectedTheme by remember { mutableStateOf(WallpaperTheme.NOIR) }
    var selectedAlignment by remember { mutableStateOf(WallpaperAlignment.CENTER) }
    var grainOpacity by remember { mutableStateOf(0.035f) }
    var showBranding by remember { mutableStateOf(true) }
    var signatureText by remember { mutableStateOf(author) }
    var selectedFont by remember { mutableStateOf(WallpaperFont.LITERATA) }

    // Text Size Control States
    val autoTextSizeSp = remember(quoteText) {
        when {
            quoteText.length < 60 -> 32f
            quoteText.length < 120 -> 26f
            quoteText.length < 200 -> 21f
            else -> 18f
        }
    }
    var isAutoTextSize by remember { mutableStateOf(true) }
    var manualTextSizeSp by remember { mutableStateOf(autoTextSizeSp) }
    val currentTextSize = if (isAutoTextSize) autoTextSizeSp else manualTextSizeSp

    // Renders Preview & Final bitmaps asynchronously
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }

    // Cleanup bitmap when leaving composition to prevent memory leaks
    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    // Trigger rendering whenever parameters change (Compose-native reactive debounce)
    LaunchedEffect(selectedTheme, selectedAlignment, grainOpacity, showBranding, currentTextSize, signatureText, selectedFont) {
        kotlinx.coroutines.delay(150) // Debounce inputs
        isRendering = true
        var clampedSize: Float? = null
        var hasVerticalOverflow = false

        val bmp = withContext(Dispatchers.Default) {
            WallpaperRenderer.renderWallpaper(
                context = context,
                width = previewWidthPx,
                height = previewHeightPx,
                quoteText = quoteText,
                signatureText = signatureText,
                theme = selectedTheme,
                alignment = selectedAlignment,
                grainOpacity = grainOpacity,
                showBranding = showBranding,
                textSizeSp = currentTextSize,
                fontChoice = selectedFont,
                onTextSizeClamped = { size ->
                    clampedSize = size
                },
                onVerticalOverflow = {
                    hasVerticalOverflow = true
                }
            )
        }

        if (clampedSize != null) {
            // Rendered at the clamped/fitted size, but we do NOT write it back to manualTextSizeSp
            // during active preview rendering to prevent locking the slider in a feedback loop.
        } else if (hasVerticalOverflow) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Quote too long for wallpaper view", Toast.LENGTH_SHORT).show()
            }
        }

        // Recycle old bitmap before assigning the new one to prevent memory leaks
        val oldBitmap = previewBitmap
        previewBitmap = bmp
        oldBitmap?.takeIf { !it.isRecycled }?.recycle()
        isRendering = false
    }

    // Bottom sheet dialog control state
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showWallpaperChoiceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val wallpaperManager = remember { WallpaperManager.getInstance(context) }
    val isWallpaperSupported = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wallpaperManager.isWallpaperSupported
        } else {
            true
        }
    }

    // WRITE_EXTERNAL_STORAGE launcher for API 28 and below
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                saveWallpaperFlow(context, quoteText, signatureText, selectedTheme, selectedAlignment, grainOpacity, showBranding, currentTextSize, selectedFont)
            }
        } else {
            Toast.makeText(context, "Storage permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    // Scaffolding UI with Theme-aware styling
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Design Wallpaper",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFFC6E20)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = Color(0xFFFC6E20)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val padding = if (isCompact) 12.dp else if (isLarge) 24.dp else 16.dp
        val spacing = if (isCompact) 10.dp else if (isLarge) 20.dp else 16.dp
        val labelSize = if (isCompact) 11.sp else if (isLarge) 14.sp else 13.sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Live Preview Canvas (Occupies top portion)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(previewWidthDp)
                        .height(previewHeightDp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF060608))
                        .border(1.dp, Color(0xFF383838), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Live Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFC8965A))
                    }

                    if (isRendering) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFC8965A), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Editor Panel Bottom Sheet Content (Occupies bottom portion)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Feature 1: Curated background picker
                Text(
                    text = "Background Preset",
                    fontSize = labelSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing / 2))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperTheme.values().forEach { theme ->
                        val isSelected = theme == selectedTheme
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFFFC6E20).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFC6E20) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTheme = theme }
                                .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 8.dp else 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFFC6E20) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Feature 1.5: Font Picker
                Text(
                    text = "Typography Style",
                    fontSize = labelSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing / 2))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperFont.values().forEach { font ->
                        val isSelected = font == selectedFont
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Color(0xFFFC6E20).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFFC6E20) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFont = font }
                                .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = if (isCompact) 8.dp else 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val buttonFontFamily = when (font) {
                                WallpaperFont.LITERATA -> LiterataFontFamily
                                WallpaperFont.INTER -> InterFontFamily
                                WallpaperFont.LORA -> FontFamily.Serif
                            }
                            val buttonFontStyle = when (font) {
                                WallpaperFont.LORA -> androidx.compose.ui.text.font.FontStyle.Italic
                                else -> androidx.compose.ui.text.font.FontStyle.Normal
                            }
                            Text(
                                text = font.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = buttonFontFamily,
                                fontStyle = buttonFontStyle,
                                color = if (isSelected) Color(0xFFFC6E20) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Feature 2: Alignments toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alignment",
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WallpaperAlignment.values().forEach { align ->
                            val isSelected = align == selectedAlignment
                            val icon = when (align) {
                                WallpaperAlignment.LEFT -> Icons.Default.FormatAlignLeft
                                WallpaperAlignment.CENTER -> Icons.Default.FormatAlignCenter
                                WallpaperAlignment.RIGHT -> Icons.Default.FormatAlignRight
                            }
                            IconButton(
                                onClick = { selectedAlignment = align },
                                modifier = Modifier
                                    .size(if (isCompact) 30.dp else 36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFFFC6E20).copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFC6E20).copy(alpha = 0.4f)
                                        else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = align.name,
                                    tint = if (isSelected) Color(0xFFFC6E20) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Feature 2.5: Text Size control row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Text Size",
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(if (isCompact) 64.dp else 80.dp)
                    )

                    // A- button
                    IconButton(
                        onClick = {
                            if (!isAutoTextSize && manualTextSizeSp > 14f) {
                                manualTextSizeSp = (manualTextSizeSp - 1f).coerceIn(14f, 40f)
                            } else if (isAutoTextSize) {
                                isAutoTextSize = false
                                manualTextSizeSp = (autoTextSizeSp - 1f).coerceIn(14f, 40f)
                            }
                        },
                        enabled = !(!isAutoTextSize && manualTextSizeSp <= 14f),
                        modifier = Modifier.size(if (isCompact) 28.dp else 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Text Size",
                            tint = if (!isAutoTextSize && manualTextSizeSp <= 14f) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Slider
                    Slider(
                        value = currentTextSize,
                        onValueChange = {
                            isAutoTextSize = false
                            manualTextSizeSp = it.roundToInt().toFloat()
                        },
                        valueRange = 14f..40f,
                        steps = 25,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isAutoTextSize) Color.Gray else Color(0xFFFC6E20),
                            activeTrackColor = if (isAutoTextSize) Color.Gray.copy(alpha = 0.5f) else Color(0xFFFC6E20),
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // A+ button
                    IconButton(
                        onClick = {
                            if (!isAutoTextSize && manualTextSizeSp < 40f) {
                                manualTextSizeSp = (manualTextSizeSp + 1f).coerceIn(14f, 40f)
                            } else if (isAutoTextSize) {
                                isAutoTextSize = false
                                manualTextSizeSp = (autoTextSizeSp + 1f).coerceIn(14f, 40f)
                            }
                        },
                        enabled = !(!isAutoTextSize && manualTextSizeSp >= 40f),
                        modifier = Modifier.size(if (isCompact) 28.dp else 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Text Size",
                            tint = if (!isAutoTextSize && manualTextSizeSp >= 40f) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Value Readout
                    Text(
                        text = "${currentTextSize.roundToInt()}sp",
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = Color(0xFFFC6E20),
                        modifier = Modifier.width(if (isCompact) 32.dp else 40.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Auto reset chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isAutoTextSize) Color(0xFFFC6E20)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                isAutoTextSize = true
                                manualTextSizeSp = autoTextSizeSp
                            }
                            .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 4.dp else 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Auto",
                            fontSize = if (isCompact) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoTextSize) Color(0xFF121212) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing))

                // Feature 3: Grain Opacity Slider (Single line)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Film Grain",
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = grainOpacity,
                        onValueChange = { grainOpacity = it },
                        valueRange = 0f..0.08f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFC6E20),
                            activeTrackColor = Color(0xFFFC6E20),
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = if (isCompact) 8.dp else 16.dp)
                    )
                    Text(
                        text = String.format("%.1f%%", grainOpacity * 100),
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = Color(0xFFFC6E20)
                    )
                }

                Spacer(modifier = Modifier.height(spacing))

                // Feature 4: Toggle branding Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Branding",
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = showBranding,
                        onCheckedChange = { showBranding = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF121212),
                            checkedTrackColor = Color(0xFFFC6E20),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(spacing))

                // Feature 4.5: Signature / Edit Text Option
                Text(
                    text = "Edit Signature",
                    fontSize = labelSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing / 2))
                OutlinedTextField(
                    value = signatureText,
                    onValueChange = { signatureText = it },
                    placeholder = { Text("None (Empty)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFC6E20),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFFC6E20),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(spacing * 1.5f))

                // Feature 5: Action Triggers Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                scope.launch {
                                    saveWallpaperFlow(context, quoteText, signatureText, selectedTheme, selectedAlignment, grainOpacity, showBranding, currentTextSize, selectedFont)
                                }
                            } else {
                                val writePermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                                if (writePermission == PackageManager.PERMISSION_GRANTED) {
                                    scope.launch {
                                        saveWallpaperFlow(context, quoteText, signatureText, selectedTheme, selectedAlignment, grainOpacity, showBranding, currentTextSize, selectedFont)
                                    }
                                } else {
                                    val activity = context as? Activity
                                    val shouldShowRationale = activity?.let {
                                        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                            it,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )
                                    } ?: false

                                    if (shouldShowRationale) {
                                        showPermissionRationaleDialog = true
                                    } else {
                                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1D17)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF7DDD4))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF7DDD4))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                shareWallpaperFlow(context, quoteText, signatureText, selectedTheme, selectedAlignment, grainOpacity, showBranding, currentTextSize, selectedFont)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1D17)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF7DDD4))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF7DDD4))
                    }
                }

                if (isWallpaperSupported) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                showWallpaperChoiceDialog = true
                            } else {
                                scope.launch {
                                    setWallpaperFlow(
                                        context = context,
                                        snackbarHostState = snackbarHostState,
                                        text = quoteText,
                                        signatureText = signatureText,
                                        theme = selectedTheme,
                                        align = selectedAlignment,
                                        grain = grainOpacity,
                                        branding = showBranding,
                                        textSizeSp = currentTextSize,
                                        fontChoice = selectedFont,
                                        which = 0
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFC6E20),
                            contentColor = Color(0xFF581E00)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SET AS WALLPAPER", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = {
                Text(
                    "Storage Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF7DDD4)
                )
            },
            text = {
                Text(
                    "To save the wallpaper to your device gallery, Focus needs access to your device storage. Please grant storage access on the next screen.",
                    fontSize = 14.sp,
                    color = Color(0xFFE1BFB2)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFC6E20),
                        contentColor = Color(0xFF581E00)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant Permission", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionRationaleDialog = false }
                ) {
                    Text("Cancel", color = Color(0xFFE1BFB2).copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF262626),
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showWallpaperChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperChoiceDialog = false },
            title = {
                Text(
                    "Set wallpaper as:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFF7DDD4)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lock Screen Button
                    Button(
                        onClick = {
                            showWallpaperChoiceDialog = false
                            scope.launch {
                                setWallpaperFlow(
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    text = quoteText,
                                    signatureText = signatureText,
                                    theme = selectedTheme,
                                    align = selectedAlignment,
                                    grain = grainOpacity,
                                    branding = showBranding,
                                    textSizeSp = currentTextSize,
                                    fontChoice = selectedFont,
                                    which = WallpaperManager.FLAG_LOCK
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1D17)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF7DDD4))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Screen", color = Color(0xFFF7DDD4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Home Screen Button
                    Button(
                        onClick = {
                            showWallpaperChoiceDialog = false
                            scope.launch {
                                setWallpaperFlow(
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    text = quoteText,
                                    signatureText = signatureText,
                                    theme = selectedTheme,
                                    align = selectedAlignment,
                                    grain = grainOpacity,
                                    branding = showBranding,
                                    textSizeSp = currentTextSize,
                                    fontChoice = selectedFont,
                                    which = WallpaperManager.FLAG_SYSTEM
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1D17)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF383838))
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF7DDD4))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Home Screen", color = Color(0xFFF7DDD4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Both Button
                    Button(
                        onClick = {
                            showWallpaperChoiceDialog = false
                            scope.launch {
                                setWallpaperFlow(
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    text = quoteText,
                                    signatureText = signatureText,
                                    theme = selectedTheme,
                                    align = selectedAlignment,
                                    grain = grainOpacity,
                                    branding = showBranding,
                                    textSizeSp = currentTextSize,
                                    fontChoice = selectedFont,
                                    which = WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFC6E20),
                            contentColor = Color(0xFF581E00)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF581E00))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Both", color = Color(0xFF581E00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showWallpaperChoiceDialog = false }
                ) {
                    Text("Cancel", color = Color(0xFFE1BFB2).copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF262626),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Saves a compiled high-resolution bitmap directly to Android Scoped Storage Pictures folder.
 */
private suspend fun saveWallpaperFlow(
    context: Context,
    text: String,
    signatureText: String,
    theme: WallpaperTheme,
    align: WallpaperAlignment,
    grain: Float,
    branding: Boolean,
    textSizeSp: Float,
    fontChoice: WallpaperFont
) {
    withContext(Dispatchers.Default) {
        val width = maxOf(1080, context.resources.displayMetrics.widthPixels)
        val height = (width * 16) / 9

        val bitmap = WallpaperRenderer.renderWallpaper(
            context = context,
            width = width,
            height = height,
            quoteText = text,
            signatureText = signatureText,
            theme = theme,
            alignment = align,
            grainOpacity = grain,
            showBranding = branding,
            textSizeSp = textSizeSp,
            fontChoice = fontChoice
        )

        if (bitmap == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Rendering failed due to low memory.", Toast.LENGTH_LONG).show()
            }
            return@withContext
        }

        try {
            val filename = "focus_wallpaper_${System.currentTimeMillis()}.png"
            var success = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Focus Wallpapers")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Focus Wallpapers")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, filename)
                FileOutputStream(file).use { out ->
                    success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                // Scan via media connection for older APIs
                android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            }

            bitmap.recycle() // Safeguard heap to prevent OOM
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Saved to your gallery ✦", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Save failed. Check storage permissions.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Save failed. Check storage permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

/**
 * Renders full-resolution wallpaper bitmap, writes to local cache, and triggers android sharing chooser.
 */
private suspend fun shareWallpaperFlow(
    context: Context,
    text: String,
    signatureText: String,
    theme: WallpaperTheme,
    align: WallpaperAlignment,
    grain: Float,
    branding: Boolean,
    textSizeSp: Float,
    fontChoice: WallpaperFont
) {
    withContext(Dispatchers.Default) {
        val width = maxOf(1080, context.resources.displayMetrics.widthPixels)
        val height = (width * 16) / 9

        val bitmap = WallpaperRenderer.renderWallpaper(
            context = context,
            width = width,
            height = height,
            quoteText = text,
            signatureText = signatureText,
            theme = theme,
            alignment = align,
            grainOpacity = grain,
            showBranding = branding,
            textSizeSp = textSizeSp,
            fontChoice = fontChoice
        ) ?: return@withContext

        val savedFile = WallpaperRenderer.saveBitmapToGallery(context, bitmap)
        bitmap.recycle()

        if (savedFile != null) {
            withContext(Dispatchers.Main) {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        savedFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Wallpaper via"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Sharing failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

/**
 * Renders full-resolution wallpaper bitmap, writes to a temporary file in app internal cache directory,
 * and sets it using WallpaperManager.
 */
private suspend fun setWallpaperFlow(
    context: Context,
    snackbarHostState: SnackbarHostState,
    text: String,
    signatureText: String,
    theme: WallpaperTheme,
    align: WallpaperAlignment,
    grain: Float,
    branding: Boolean,
    textSizeSp: Float,
    fontChoice: WallpaperFont,
    which: Int
) {
    withContext(Dispatchers.Default) {
        val width = maxOf(1080, context.resources.displayMetrics.widthPixels)
        val height = (width * 16) / 9

        val file = File(context.cacheDir, "focus_wallpaper_temp.png")
        var bitmap: Bitmap? = null
        try {
            bitmap = WallpaperRenderer.renderWallpaper(
                context = context,
                width = width,
                height = height,
                quoteText = text,
                signatureText = signatureText,
                theme = theme,
                alignment = align,
                grainOpacity = grain,
                showBranding = branding,
                textSizeSp = textSizeSp,
                fontChoice = fontChoice
            )

            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Failed. Try saving to gallery and setting manually.")
                }
                return@withContext
            }

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = if (which == 0) (WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM) else which
                wm.setBitmap(bitmap, null, true, flags)
            } else {
                wm.setBitmap(bitmap)
            }

            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Wallpaper set ✦")
            }
        } catch (oom: OutOfMemoryError) {
            System.gc()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Not enough memory", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Failed. Try saving to gallery and setting manually.")
            }
        } finally {
            bitmap?.recycle()
            if (file.exists()) {
                file.delete()
            }
        }
    }
}

