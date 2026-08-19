package com.example.motivation.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.motivation.R
import com.example.motivation.getGreeting
import com.example.motivation.viewmodel.MainViewModel
import com.example.motivation.viewmodel.FocusGuardViewModel
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.EmojiEvents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import com.example.motivation.ui.theme.PlaywriteGBSFontFamily
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import kotlinx.coroutines.launch
import com.example.motivation.viewmodel.ProfileViewModel
import com.example.motivation.ui.ProfileAvatar
import com.example.motivation.ui.BadgeIcon
import com.example.motivation.ui.getHighestTierUnlockedBadge

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(navController: NavController, userName: String, mainViewModel: MainViewModel = viewModel(), profileViewModel: ProfileViewModel = viewModel()) {
    val quote by mainViewModel.quote.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val isFavorite = remember(quote, favorites) { favorites.contains(quote) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val userProfilePhotoUri by profileViewModel.userProfilePhotoUri.collectAsState()
    val enableBadgeDisplay by profileViewModel.enableBadgeDisplay.collectAsState()
    val achievements by profileViewModel.achievements.collectAsState()
    val streakCount by profileViewModel.streakCount.collectAsState()
    
    var showQuickProfile by remember { mutableStateOf(false) }

    val prefs = remember(context) { context.getSharedPreferences("motivation_prefs", android.content.Context.MODE_PRIVATE) }
    val quickWallpaperEnabled = prefs.getBoolean("quick_wallpaper_on_hold", true)

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTabletOrFoldable = screenWidthDp > 600

    var showShareDialog by remember { mutableStateOf(false) }

    val selectedQuotes = remember { mutableStateListOf<com.example.motivation.model.Quote>() }
    val isInSelectionMode = selectedQuotes.isNotEmpty()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var dragOffset by remember { mutableStateOf(0f) }
    val animOffset = remember { Animatable(0f) }
    val displayOffset = if (animOffset.isRunning) animOffset.value else dragOffset

    BackHandler(enabled = isInSelectionMode) {
        selectedQuotes.clear()
    }

    if (showShareDialog && quote != null && quote!!.text.isNotEmpty()) {
        val targetQuote = quote!!
        val isFav = favorites.contains(targetQuote)
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                Text(
                    text = "Quote Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            text = {
                Text(
                    text = "Choose an action for this quote:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showShareDialog = false
                            mainViewModel.toggleFavorite(targetQuote)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isFav) "Remove from Favorites" else "Add to Favorites",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            showShareDialog = false
                            shareQuote(context, "${targetQuote.text} - Focus")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        border = BoxDefaults.cardBorder()
                    ) {
                        Text("Share as Text", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showShareDialog = false
                            clipboardManager.setText(AnnotatedString("${targetQuote.text} - Focus"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        border = BoxDefaults.cardBorder()
                    ) {
                        Text("Copy to Clipboard", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showShareDialog = false
                            val encodedQuote = java.net.URLEncoder.encode(targetQuote.text, "UTF-8")
                            navController.navigate("wallpaper_generator?quoteText=$encodedQuote&author=Focus")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        border = BoxDefaults.cardBorder()
                    ) {
                        Text("Create Wallpaper", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showShareDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedQuotes.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedQuotes.clear() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Selections"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val count = selectedQuotes.size
                                mainViewModel.favoriteQuotes(selectedQuotes.toList())
                                selectedQuotes.clear()
                                scope.launch {
                                    snackbarHostState.showSnackbar("$count quotes added to favorites")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite All"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedQuotes.isNotEmpty()) {
                                    val combinedText = selectedQuotes.joinToString("\n\n") { "\"${it.text}\" - Focus" }
                                    shareQuote(context, combinedText)
                                    selectedQuotes.clear()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedQuotes.isNotEmpty()) {
                                    val count = selectedQuotes.size
                                    val combinedText = selectedQuotes.joinToString("\n\n") { "\"${it.text}\" - Focus" }
                                    clipboardManager.setText(AnnotatedString(combinedText))
                                    selectedQuotes.clear()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("$count quotes copied")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.secondary,
                        navigationIconContentColor = MaterialTheme.colorScheme.secondary,
                        actionIconContentColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Personalized Greeting & Profile Avatar Row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getGreeting(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Profile Photo + Badge Overlay
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { showQuickProfile = true }
                ) {
                    ProfileAvatar(
                        photoUriString = userProfilePhotoUri,
                        userName = userName,
                        size = 42.dp
                    )
                    if (enableBadgeDisplay) {
                        val highestBadge = getHighestTierUnlockedBadge(achievements)
                        if (highestBadge != null) {
                            BadgeIcon(
                                icon = getAchievementIcon(highestBadge.achievementId),
                                tier = highestBadge.tier,
                                size = 18.dp,
                                isUnlocked = true,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 3.dp, y = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Quote Card
            val quoteFontSize = if (isTabletOrFoldable) 42.sp else 35.sp
            val quoteLineHeight = if (isTabletOrFoldable) 56.sp else 48.sp
            val cardMinHeight = if (isTabletOrFoldable) 300.dp else 250.dp

            val isSelected = quote?.let { selectedQuotes.contains(it) } ?: false
            val cardBorder = if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFC6E20))
            } else {
                BoxDefaults.cardBorder()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = cardMinHeight)
                    .scale(if (isSelected) 0.97f else 1f)
                    .offset { IntOffset(displayOffset.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val threshold = 150f
                                if (dragOffset > threshold) {
                                    mainViewModel.showPreviousQuote()
                                } else if (dragOffset < -threshold) {
                                    mainViewModel.showNextQuote()
                                }
                                scope.launch {
                                    animOffset.snapTo(dragOffset)
                                    dragOffset = 0f
                                    animOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    animOffset.snapTo(dragOffset)
                                    dragOffset = 0f
                                    animOffset.animateTo(0f)
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            }
                        )
                    }
                    .combinedClickable(
                        onLongClick = {
                            quote?.let {
                                if (it.text.isNotEmpty()) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (quickWallpaperEnabled) {
                                        val encodedQuote = java.net.URLEncoder.encode(it.text, "UTF-8")
                                        navController.navigate("wallpaper_generator?quoteText=$encodedQuote&author=Focus")
                                    } else {
                                        showShareDialog = true
                                    }
                                }
                            }
                        },
                        onDoubleClick = {
                            quote?.let { currentQuote ->
                                if (isInSelectionMode) {
                                    if (selectedQuotes.contains(currentQuote)) {
                                        selectedQuotes.remove(currentQuote)
                                    } else {
                                        selectedQuotes.add(currentQuote)
                                    }
                                } else {
                                    selectedQuotes.add(currentQuote)
                                }
                            }
                        },
                        onClick = {
                            quote?.let { currentQuote ->
                                if (isInSelectionMode) {
                                    if (selectedQuotes.contains(currentQuote)) {
                                        selectedQuotes.remove(currentQuote)
                                    } else {
                                        selectedQuotes.add(currentQuote)
                                    }
                                } else {
                                    showShareDialog = true
                                }
                            }
                        }
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = cardMinHeight)) {
                    if (isInSelectionMode && isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0xFFFC6E20).copy(alpha = 0.08f))
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_quote_mark),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedContent(
                            targetState = quote,
                            transitionSpec = {
                                fadeIn(tween(400)).togetherWith(fadeOut(tween(400)))
                            },
                            label = "QuoteAnim"
                        ) { targetQuote ->
                            Text(
                                text = targetQuote?.text ?: "Loading...",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    lineHeight = quoteLineHeight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = PlaywriteGBSFontFamily,
                                    fontSize = quoteFontSize
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = quote?.category?.uppercase() ?: "",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isInSelectionMode) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = Color(0xFFFC6E20),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(24.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(24.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionItem(icon = Icons.Default.Refresh, label = "Refresh") {
                    mainViewModel.refreshQuote()
                }
                ActionItem(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                ) {
                    quote?.let { mainViewModel.toggleFavorite(it) }
                }
                ActionItem(icon = Icons.Default.Share, label = "Share") {
                    if (quote != null && quote.text.isNotEmpty()) {
                        showShareDialog = true
                    }
                }
                ActionItem(icon = Icons.Default.ContentCopy, label = "Copy") {
                    quote?.let { clipboardManager.setText(AnnotatedString("${it.text} - Focus")) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))



            // Explore Section
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val exploreItems = listOf(
                ExploreItemData("Favorites", "favorites", icon = Icons.Default.FavoriteBorder),
                ExploreItemData("History", "history", icon = Icons.Default.History),
                ExploreItemData("Search", "search", icon = Icons.Default.Search),
                ExploreItemData("Mood", "mood", icon = Icons.Default.SentimentSatisfiedAlt),
                ExploreItemData("Journal", "journal", icon = Icons.Default.Book),
                ExploreItemData("Focus Guard", "focus_guard/0", iconRes = R.drawable.shield_locked_24),
                ExploreItemData("Streak", "streak", iconRes = R.drawable.ic_streak),
                ExploreItemData("Achievements", "achievements", iconRes = R.drawable.ic_achievements),                             
                ExploreItemData("Settings", "settings", icon = Icons.Default.Settings)
            )

            val columns = when {
                screenWidthDp > 840 -> 6
                screenWidthDp > 600 -> 4
                else -> 3
            }

            val rows = exploreItems.chunked(columns)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (rowItems in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (item in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                ExploreCard(item) {
                                    navController.navigate(item.route)
                                }
                            }
                        }
                        if (rowItems.size < columns) {
                            val emptySlots = columns - rowItems.size
                            for (i in 0 until emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showQuickProfile) {
        val unlockedCount = remember(achievements) {
            achievements.count { it.isUnlocked }
        }
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showQuickProfile = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Avatar + Badge Overlay
                Box(modifier = Modifier.size(100.dp)) {
                    ProfileAvatar(
                        photoUriString = userProfilePhotoUri,
                        userName = userName,
                        size = 100.dp
                    )
                    if (enableBadgeDisplay) {
                        val highestBadge = getHighestTierUnlockedBadge(achievements)
                        if (highestBadge != null) {
                            BadgeIcon(
                                icon = getAchievementIcon(highestBadge.achievementId),
                                tier = highestBadge.tier,
                                size = 32.dp,
                                isUnlocked = true,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = userName.ifBlank { "Focus User" },
                    fontFamily = com.example.motivation.ui.theme.LiterataFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = com.example.motivation.ui.theme.VibrantOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$streakCount-Day Streak",
                        fontFamily = com.example.motivation.ui.theme.InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = com.example.motivation.ui.theme.VibrantOrange
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$unlockedCount Badges Unlocked",
                        fontFamily = com.example.motivation.ui.theme.InterFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showQuickProfile = false
                            navController.navigate("my_profile")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.motivation.ui.theme.VibrantOrange,
                            contentColor = Color(0xFF121212)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Go to Full Profile",
                            fontFamily = com.example.motivation.ui.theme.LiterataFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showQuickProfile = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontFamily = com.example.motivation.ui.theme.LiterataFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}

@Composable
fun ActionItem(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.secondary, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ExploreCard(item: ExploreItemData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BoxDefaults.cardBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (item.iconRes != null) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

data class ExploreItemData(
    val title: String,
    val route: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null
)

fun shareQuote(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
