package com.example.motivation.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import android.view.ViewGroup
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.viewmodel.MoodDayPoint
import com.example.motivation.viewmodel.ReportCardViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportCardScreen(
    navController: NavController,
    isPartialWeekParam: Boolean = false,
    viewModel: ReportCardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val darkBlack = MaterialTheme.colorScheme.background
    val creamBeige = MaterialTheme.colorScheme.onBackground
    val orange = com.example.motivation.ui.theme.VibrantOrange
    val charcoalGray = MaterialTheme.colorScheme.surface

    LaunchedEffect(isPartialWeekParam) {
        // Trigger report generation if needed
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlack)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = orange,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val weekOffset by viewModel.weekOffset.collectAsState()

                val saveReportAction = {
                    coroutineScope.launch {
                        Toast.makeText(context, "Saving report image...", Toast.LENGTH_SHORT).show()
                        val bitmap = generateReportBitmap(context, view, uiState, weekOffset, isPartialWeekParam)
                        if (bitmap != null) {
                            val success = saveReportToGallery(context, bitmap)
                            bitmap.recycle()
                            if (success) {
                                Toast.makeText(context, "Saved to your gallery ✦", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Save failed. Check storage permissions.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to generate report image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        saveReportAction()
                    } else {
                        Toast.makeText(context, "Storage permission denied.", Toast.LENGTH_LONG).show()
                    }
                }

                // Outer scrollable container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .background(darkBlack)
                ) {
                    ReportCardRenderContent(
                        uiState = uiState,
                        weekOffset = weekOffset,
                        isPartialWeekParam = isPartialWeekParam,
                        onPreviousWeekClick = { viewModel.selectPreviousWeek() },
                        onNextWeekClick = { viewModel.selectNextWeek() },
                        showArrows = true
                    )
                }

                // Bottom Action buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = darkBlack,
                    border = BorderStroke(1.dp, creamBeige.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Close Button
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = creamBeige),
                            border = BorderStroke(1.dp, creamBeige.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Close",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }

                        // Save Button (Filled/Primary action)
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    saveReportAction()
                                } else {
                                    val writePermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                    if (writePermission == PackageManager.PERMISSION_GRANTED) {
                                        saveReportAction()
                                    } else {
                                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = orange,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = "Save"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = LiterataFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportCardRenderContent(
    uiState: com.example.motivation.viewmodel.ReportCardUiState,
    weekOffset: Int,
    isPartialWeekParam: Boolean,
    onPreviousWeekClick: () -> Unit = {},
    onNextWeekClick: () -> Unit = {},
    showArrows: Boolean = true
) {
    val darkBlack = MaterialTheme.colorScheme.background
    val creamBeige = MaterialTheme.colorScheme.onBackground
    val orange = com.example.motivation.ui.theme.VibrantOrange
    val charcoalGray = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(darkBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Soft Warning Banner for partial week
        if (uiState.isPartialWeek || isPartialWeekParam) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = orange.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, orange.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Warning",
                        tint = orange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your week isn't over yet — this is a partial snapshot",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LiterataFontFamily),
                        color = creamBeige,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Header
        Text(
            text = "Your Week in Focus ✦",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = creamBeige
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showArrows) {
                IconButton(onClick = onPreviousWeekClick) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous Week",
                        tint = orange,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Subtitle (Date range)
            Text(
                text = uiState.formattedDateRange,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                ),
                color = creamBeige.copy(alpha = 0.9f)
            )

            if (showArrows) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onNextWeekClick,
                    enabled = weekOffset > 0
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next Week",
                        tint = if (weekOffset > 0) orange else creamBeige.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Divider(color = creamBeige.copy(alpha = 0.1f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // 1. MOOD CURVE GRAPH
        Text(
            text = "Weekly Mood Curve",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = creamBeige,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        MoodCurveChart(
            moodCurveData = uiState.moodCurveData,
            orangeColor = orange,
            creamColor = creamBeige,
            charcoalColor = charcoalGray
        )

        Spacer(modifier = Modifier.height(20.dp))
        Divider(color = creamBeige.copy(alpha = 0.1f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(20.dp))

        // 2. MOST USED WORDS
        Text(
            text = "Words on your mind this week",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = creamBeige,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (uiState.topWords.isEmpty()) {
            Text(
                text = "No journal entries this week ✦",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontStyle = FontStyle.Italic
                ),
                color = creamBeige.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.topWords.forEach { word ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(charcoalGray)
                            .border(1.dp, creamBeige.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = creamBeige
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = creamBeige.copy(alpha = 0.1f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(24.dp))

        // 3. STREAK STAT CARDS
        Text(
            text = "Weekly Highlights",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = creamBeige,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Streak
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Whatshot,
                label = "Streak",
                value = "${uiState.currentStreak} Days",
                borderColor = orange,
                containerColor = charcoalGray,
                textColor = creamBeige
            )

            // Card 2: Journal Entries
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.MenuBook,
                label = "Journals",
                value = "${uiState.journalEntryCount} Entry${if (uiState.journalEntryCount == 1) "" else "s"}",
                borderColor = orange,
                containerColor = charcoalGray,
                textColor = creamBeige
            )

            // Card 3: Avg Mood
            StatCard(
                modifier = Modifier.weight(1.1f),
                icon = getMoodIcon(uiState.avgMoodEmoji),
                label = "Avg Mood",
                value = uiState.avgMoodLabel,
                borderColor = orange,
                containerColor = charcoalGray,
                textColor = creamBeige
            )
        }

        Spacer(modifier = Modifier.height(24.dp))     }
    }

@Composable
fun MoodCurveChart(
    moodCurveData: List<MoodDayPoint>,
    orangeColor: Color,
    creamColor: Color,
    charcoalColor: Color
) {
    val textMeasurer = rememberTextMeasurer()

    val happyPainter = rememberVectorPainter(Icons.Rounded.SentimentVerySatisfied)
    val inspiredPainter = rememberVectorPainter(Icons.Rounded.AutoAwesome)
    val calmPainter = rememberVectorPainter(Icons.Rounded.SelfImprovement)
    val neutralPainter = rememberVectorPainter(Icons.Rounded.SentimentNeutral)
    val sadPainter = rememberVectorPainter(Icons.Rounded.SentimentVeryDissatisfied)
    val tiredPainter = rememberVectorPainter(Icons.Rounded.Bedtime)
    val angryPainter = rememberVectorPainter(Icons.Rounded.Whatshot)
    val anxiousPainter = rememberVectorPainter(Icons.Rounded.Psychology)
    val defaultPainter = rememberVectorPainter(Icons.Rounded.AutoAwesome)

    fun getPainterForEmoji(emoji: String): VectorPainter {
        return when (emoji) {
            "😊" -> happyPainter
            "✨" -> inspiredPainter
            "🧘" -> calmPainter
            "😐" -> neutralPainter
            "😔" -> sadPainter
            "😴" -> tiredPainter
            "😡" -> angryPainter
            "🥺" -> anxiousPainter
            else -> defaultPainter
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(charcoalColor.copy(alpha = 0.4f))
            .border(1.dp, creamColor.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
    ) {
        val width = size.width
        val height = size.height

        val minScore = 1f
        val maxScore = 5f
        val paddingY = 32.dp.toPx()
        val paddingX = 42.dp.toPx()
        val usableHeight = height - 2 * paddingY
        val usableWidth = width - 2 * paddingX

        fun getYForScore(score: Float): Float {
            return paddingY + usableHeight * (1f - (score - minScore) / (maxScore - minScore))
        }

        // Draw 5 horizontal guide lines for mood scores
        (1..5).forEach { score ->
            val y = getYForScore(score.toFloat())
            drawLine(
                color = creamColor.copy(alpha = 0.06f),
                start = Offset(paddingX, y),
                end = Offset(width - paddingX, y),
                strokeWidth = 1.dp.toPx()
            )
            // Label on the left
            val label = when (score) {
                5 -> "Amazing"
                4 -> "Good"
                3 -> "Neutral"
                2 -> "Bad"
                1 -> "Awful"
                else -> ""
            }
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = TextStyle(
                    color = creamColor.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontFamily = LiterataFontFamily
                ),
                topLeft = Offset(8.dp.toPx(), y - 7.dp.toPx())
            )
        }

        // 7 days Mon-Sun X mapping
        val pointsCount = 7
        val xSpacing = usableWidth / (pointsCount - 1)

        val activePoints = mutableListOf<Offset>()
        moodCurveData.forEachIndexed { idx, point ->
            val x = paddingX + idx * xSpacing
            if (point.score != null) {
                val y = getYForScore(point.score)
                activePoints.add(Offset(x, y))
            }
        }

        // Draw smooth bezier curve & vertical gradient underneath
        if (activePoints.size >= 2) {
            val path = Path()
            val fillPath = Path()

            path.moveTo(activePoints[0].x, activePoints[0].y)
            fillPath.moveTo(activePoints[0].x, activePoints[0].y)

            for (i in 0 until activePoints.size - 1) {
                val p0 = activePoints[i]
                val p1 = activePoints[i + 1]

                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                val controlY2 = p1.y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            drawPath(
                path = path,
                color = orangeColor,
                style = Stroke(width = 3.5f.dp.toPx(), cap = StrokeCap.Round)
            )

            fillPath.lineTo(activePoints.last().x, height - paddingY)
            fillPath.lineTo(activePoints.first().x, height - paddingY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(orangeColor.copy(alpha = 0.22f), Color.Transparent),
                    startY = activePoints.minOfOrNull { it.y } ?: 0f,
                    endY = height - paddingY
                )
            )
        }

        // Draw point circles, dashed empty states, emojis, and Mon-Sun day labels
        moodCurveData.forEachIndexed { idx, point ->
            val x = paddingX + idx * xSpacing

            // Draw day labels at the bottom
            val textLayoutResult = textMeasurer.measure(
                text = point.dayName,
                style = TextStyle(
                    color = creamColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = LiterataFontFamily
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x - textLayoutResult.size.width / 2f, height - 20.dp.toPx())
            )

            if (point.score != null) {
                val y = getYForScore(point.score)

                // Glowing outer circle
                drawCircle(
                    color = orangeColor.copy(alpha = 0.3f),
                    radius = 10.dp.toPx(),
                    center = Offset(x, y)
                )
                // Inner solid point
                drawCircle(
                    color = orangeColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )

                // Floating mood vector icon instead of emoji
                point.emoji?.let { emoji ->
                    val painter = getPainterForEmoji(emoji)
                    val sizePx = 18.dp.toPx()
                    translate(left = x - sizePx / 2f, top = y - 24.dp.toPx()) {
                        with(painter) {
                            draw(
                                size = Size(sizePx, sizePx),
                                colorFilter = ColorFilter.tint(orangeColor)
                            )
                        }
                    }
                }
            } else {
                // Subtle dashed circle at Neutral score (3) for empty days
                val y = getYForScore(3f)
                drawCircle(
                    color = creamColor.copy(alpha = 0.12f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                )
            }
        }
    }
}

private fun getMoodIcon(emoji: String): ImageVector {
    return when (emoji) {
        "😊" -> Icons.Rounded.SentimentVerySatisfied
        "😌" -> Icons.Rounded.SentimentSatisfied
        "😐" -> Icons.Rounded.SentimentNeutral
        "😔" -> Icons.Rounded.SentimentVeryDissatisfied
        "😡" -> Icons.Rounded.Whatshot
        else -> Icons.Rounded.SentimentNeutral
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    borderColor: Color,
    containerColor: Color,
    textColor: Color
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFC6E20),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = textColor.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = textColor
            )
        }
    }
}


private suspend fun generateReportBitmap(
    context: Context,
    view: View,
    uiState: com.example.motivation.viewmodel.ReportCardUiState,
    weekOffset: Int,
    isPartialWeekParam: Boolean
): Bitmap? {
    val rootView = view.rootView as? ViewGroup ?: return null
    val composeView = ComposeView(context).apply {
        visibility = View.INVISIBLE
        layoutParams = ViewGroup.LayoutParams(
            view.width,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setContent {
            ReportCardRenderContent(
                uiState = uiState,
                weekOffset = weekOffset,
                isPartialWeekParam = isPartialWeekParam,
                showArrows = false
            )
        }
    }
    
    rootView.addView(composeView)
    
    // Allow the Compose view tree to fully recompose and lay out
    kotlinx.coroutines.delay(300)
    
    return try {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
        
        if (composeView.measuredWidth > 0 && composeView.measuredHeight > 0) {
            val bitmap = Bitmap.createBitmap(
                composeView.measuredWidth,
                composeView.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            composeView.draw(canvas)
            bitmap
        } else {
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("ShareReport", "Error rendering offscreen view", e)
        null
    } finally {
        rootView.removeView(composeView)
    }
}

private fun saveReportToGallery(context: Context, bitmap: Bitmap): Boolean {
    val filename = "focus_mood_report_${System.currentTimeMillis()}.png"
    var success = false
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Focus Reports")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Focus Reports"
            )
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, filename)
            FileOutputStream(file).use { out ->
                success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }
    } catch (e: Exception) {
        android.util.Log.e("SaveReport", "Failed to save report to gallery", e)
    }
    return success
}
