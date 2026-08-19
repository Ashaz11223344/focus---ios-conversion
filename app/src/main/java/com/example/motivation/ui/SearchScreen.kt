package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.viewmodel.MainViewModel

import androidx.navigation.NavController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(navController: NavController, mainViewModel: MainViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val searchResults by mainViewModel.searchResults.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val suggestions by mainViewModel.searchSuggestions.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val prefs = remember(context) { context.getSharedPreferences("motivation_prefs", android.content.Context.MODE_PRIVATE) }
    val quickWallpaperEnabled = prefs.getBoolean("quick_wallpaper_on_hold", true)
    
    var shareTargetQuote by remember { mutableStateOf<com.example.motivation.model.Quote?>(null) }
    
    // State to hold selected quotes
    val selectedQuotes = remember { mutableStateListOf<com.example.motivation.model.Quote>() }
    val isInSelectionMode = selectedQuotes.isNotEmpty()

    // Handle back press to clear selection
    BackHandler(enabled = isInSelectionMode) {
        selectedQuotes.clear()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val categories = mainViewModel.categories

    LaunchedEffect(Unit) {
        mainViewModel.generateSuggestions(selectedCategory)
    }

    if (shareTargetQuote != null && shareTargetQuote!!.text.isNotEmpty()) {
        val quote = shareTargetQuote!!
        val isFav = favorites.contains(quote)
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
                            mainViewModel.toggleFavorite(quote)
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
                            shareQuote(context, "${quote.text} - Focus")
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
                            shareTargetQuote = null
                            clipboardManager.setText(AnnotatedString("${quote.text} - Focus"))
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
                            shareTargetQuote = null
                            val encodedQuote = java.net.URLEncoder.encode(quote.text, "UTF-8")
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
                            text = "Search",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Compact Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isEmpty()) {
                        mainViewModel.generateSuggestions(selectedCategory)
                    } else {
                        mainViewModel.search(it, selectedCategory)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { 
                    Text(
                        "Search quotes...", 
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    ) 
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.secondary,
                    unfocusedTextColor = MaterialTheme.colorScheme.secondary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Compact Categories Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { category ->
                    val isSelectedCategory = selectedCategory == category
                    Surface(
                        modifier = Modifier.clickable { 
                            selectedCategory = category
                            if (query.isEmpty()) {
                                mainViewModel.generateSuggestions(category)
                            } else {
                                mainViewModel.search(query, category)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelectedCategory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (!isSelectedCategory) BoxDefaults.cardBorder() else null
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelectedCategory) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (query.isEmpty()) {
                Text(
                    text = "Suggested for You ✦",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (suggestions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No suggestions available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(suggestions) { quote ->
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
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            if (quickWallpaperEnabled) {
                                                val encodedQuote = java.net.URLEncoder.encode(quote.text, "UTF-8")
                                                navController.navigate("wallpaper_generator?quoteText=$encodedQuote&author=Focus")
                                            } else {
                                                shareTargetQuote = quote
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
                                            } else {
                                                if (quote.text.isNotEmpty()) {
                                                    shareTargetQuote = quote
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
                                        Spacer(modifier = Modifier.height(8.dp))
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
            } else {
                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No results found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(searchResults) { quote ->
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
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            if (quickWallpaperEnabled) {
                                                val encodedQuote = java.net.URLEncoder.encode(quote.text, "UTF-8")
                                                navController.navigate("wallpaper_generator?quoteText=$encodedQuote&author=Focus")
                                            } else {
                                                shareTargetQuote = quote
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
                                            } else {
                                                if (quote.text.isNotEmpty()) {
                                                    shareTargetQuote = quote
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
                                        Spacer(modifier = Modifier.height(8.dp))
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
