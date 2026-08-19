package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

import androidx.navigation.NavController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, mainViewModel: MainViewModel = viewModel()) {
    val history by mainViewModel.history.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val prefs = remember(context) { context.getSharedPreferences("motivation_prefs", android.content.Context.MODE_PRIVATE) }
    val quickWallpaperEnabled = prefs.getBoolean("quick_wallpaper_on_hold", true)

    var shareTargetQuote by remember { mutableStateOf<com.example.motivation.model.Quote?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // State to hold selected quotes
    val selectedQuotes = remember { mutableStateListOf<com.example.motivation.model.Quote>() }
    val isInSelectionMode = selectedQuotes.isNotEmpty()
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }

    // Handle back press to clear selection
    BackHandler(enabled = isInSelectionMode) {
        selectedQuotes.clear()
    }

    if (shareTargetQuote != null && shareTargetQuote!!.text.isNotEmpty()) {
        val targetQuote = shareTargetQuote!!
        val isFav = favorites.contains(targetQuote)
        AlertDialog(
            onDismissRequest = { shareTargetQuote = null },
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
                            shareTargetQuote = null
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
                            shareTargetQuote = null
                            shareQuote(context, "${targetQuote.text} - Focus")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Share as Text", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shareTargetQuote = null
                            clipboardManager.setText(AnnotatedString("${targetQuote.text} - Focus"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Copy to Clipboard", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shareTargetQuote = null
                            val encodedQuote = java.net.URLEncoder.encode(targetQuote.text, "UTF-8")
                            navController.navigate("wallpaper_generator?quoteText=$encodedQuote&author=Focus")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create Wallpaper", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { shareTargetQuote = null },
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

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear all history?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all viewed quote records.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        coroutineScope.launch {
                            mainViewModel.clearHistory()
                            snackbarHostState.showSnackbar("History cleared")
                        }
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showRemoveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmDialog = false },
            title = {
                Text(
                    text = "Remove ${selectedQuotes.size} quotes from history?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirmDialog = false
                        val removedList = selectedQuotes.toList()
                        selectedQuotes.clear()
                        mainViewModel.removeHistory(removedList)
                        coroutineScope.launch {
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = "${removedList.size} quotes removed",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                mainViewModel.reinsertHistory(removedList)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveConfirmDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isInSelectionMode) {
                        Text(
                            text = "${selectedQuotes.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(onClick = { selectedQuotes.clear() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Selections"
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        IconButton(
                            onClick = {
                                val count = selectedQuotes.size
                                mainViewModel.favoriteQuotes(selectedQuotes.toList())
                                selectedQuotes.clear()
                                coroutineScope.launch {
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
                                    coroutineScope.launch {
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
                        IconButton(
                            onClick = { showRemoveConfirmDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Selected"
                            )
                        }
                    } else {
                        if (history.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear History"
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.secondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.secondary,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary
                )
            )
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
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No history yet.",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Quotes you view will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(history) { quote ->
                            val isFavorite = remember(quote, favorites) { favorites.contains(quote) }
                            val isSelected = selectedQuotes.contains(quote)
                            val cardBorder = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFC6E20))
                            } else {
                                BoxDefaults.cardBorder()
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(if (isSelected) 0.97f else 1f)
                                    .combinedClickable(
                                        onLongClick = {
                                            if (quote.text.isNotEmpty()) {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                if (quickWallpaperEnabled) {
                                                    val encodedQuote = java.net.URLEncoder.encode(quote.text, "UTF-8")
                                                    navController.navigate("wallpaper_generator?quoteText=$encodedQuote&author=Focus")
                                                } else {
                                                    shareTargetQuote = quote
                                                }
                                            }
                                        },
                                        onDoubleClick = {
                                            if (isInSelectionMode) {
                                                if (isSelected) {
                                                    selectedQuotes.remove(quote)
                                                } else {
                                                    selectedQuotes.add(quote)
                                                }
                                            } else {
                                                selectedQuotes.add(quote)
                                            }
                                        },
                                        onClick = {
                                            if (isInSelectionMode) {
                                                if (isSelected) {
                                                    selectedQuotes.remove(quote)
                                                } else {
                                                    selectedQuotes.add(quote)
                                                }
                                            }
                                        }
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = cardBorder
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (isInSelectionMode && isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color(0xFFFC6E20).copy(alpha = 0.08f))
                                        )
                                    }
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = quote.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 15.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontStyle = FontStyle.Italic
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(end = if (isInSelectionMode) 24.dp else 0.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = quote.category.uppercase(),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            if (!isInSelectionMode) {
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            if (quote.text.isNotEmpty()) {
                                                                shareTargetQuote = quote
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Share, 
                                                            contentDescription = "Share",
                                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { mainViewModel.toggleFavorite(quote) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            contentDescription = "Favorite",
                                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
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
                                                    .padding(12.dp)
                                                    .size(20.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp)
                                                    .size(20.dp)
                                                    .border(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                                            )
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
}
