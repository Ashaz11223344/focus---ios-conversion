package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.model.JournalEntry
import com.example.motivation.security.PinManager
import com.example.motivation.viewmodel.MainViewModel
import com.example.motivation.viewmodel.PrivateJournalViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.motivation.ui.BoxDefaults
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    mainViewModel: MainViewModel = viewModel(),
    privateViewModel: PrivateJournalViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High fidelity custom themed Tab Switcher
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.secondary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFFFC6E20) // Focus Vibrant Orange
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Journal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                selectedContentColor = Color(0xFFFC6E20),
                unselectedContentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                },
                text = {
                    Text("Private", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                },
                selectedContentColor = Color(0xFFFC6E20),
                unselectedContentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
        }

        if (selectedTab == 0) {
            JournalEntriesView(mainViewModel)
        } else {
            // Authentication and setups state machine
            val isPinSetState = remember { mutableStateOf(PinManager.isPinSet(context)) }
            val isUnlocked by privateViewModel.isUnlocked.collectAsState()

            // Dynamic refresh of PIN status upon return or reset
            LaunchedEffect(selectedTab) {
                isPinSetState.value = PinManager.isPinSet(context)
            }

            if (!isPinSetState.value) {
                PinSetupScreen(
                    onSetupComplete = {
                        isPinSetState.value = true
                    },
                    onCancel = {
                        selectedTab = 0
                    }
                )
            } else if (!isUnlocked) {
                PinUnlockScreen(
                    viewModel = privateViewModel,
                    onUnlockComplete = {
                        // Successfully authenticated, reveals list
                    },
                    onResetPin = {
                        isPinSetState.value = false
                    },
                    onCancel = {
                        selectedTab = 0
                    }
                )
            } else {
                PrivateJournalScreen(
                    viewModel = privateViewModel,
                    onBackClick = {
                        privateViewModel.lock()
                        selectedTab = 0
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JournalEntriesView(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var noteText by remember { mutableStateOf("") }
    val entries by mainViewModel.journalEntries.collectAsState()

    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<JournalEntry?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Journal",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Compact Entry Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BoxDefaults.cardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    TextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        placeholder = { 
                            Text(
                                "What's on your mind?", 
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.secondary,
                            unfocusedTextColor = MaterialTheme.colorScheme.secondary
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    
                    Button(
                        onClick = { 
                            mainViewModel.addJournalEntry(noteText)
                            noteText = ""
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Entry", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Text(
                text = "Previous Entries",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (entries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No entries yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        JournalEntryCard(
                            entry = entry,
                            modifier = Modifier.animateItemPlacement(),
                            onEditClick = { editingEntry = entry },
                            onDeleteClick = { deletingEntry = entry }
                        )
                    }
                }
            }
        }

        // Deletion Dialog
        if (deletingEntry != null) {
            val entry = deletingEntry!!
            val previewText = if (entry.content.length > 50) {
                entry.content.take(50) + "..."
            } else {
                entry.content
            }

            AlertDialog(
                onDismissRequest = { deletingEntry = null },
                title = {
                    Text(
                        "Delete this entry?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                text = {
                    Text(
                        text = "\"$previewText\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            deletingEntry = null
                            val entryToDelete = entry
                            mainViewModel.deleteJournalEntry(entryToDelete)

                            scope.launch {
                                val dismissJob = launch {
                                    delay(5000)
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                }
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    message = "Entry deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Indefinite
                                )
                                dismissJob.cancel()
                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                    mainViewModel.reinsertJournalEntry(entryToDelete)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC6E20)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deletingEntry = null }
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Edit Entry Dialog / Modal
        if (editingEntry != null) {
            val entry = editingEntry!!
            var editContent by remember(entry) { mutableStateOf(entry.content) }
            var showDiscardDialog by remember { mutableStateOf(false) }

            Dialog(
                onDismissRequest = {
                    if (editContent != entry.content) {
                        showDiscardDialog = true
                    } else {
                        editingEntry = null
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Edit Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (editContent != entry.content) {
                                        showDiscardDialog = true
                                    } else {
                                        editingEntry = null
                                    }
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.secondary,
                                navigationIconContentColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = entry.dateDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BoxDefaults.cardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                TextField(
                                    value = editContent,
                                    onValueChange = { editContent = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    placeholder = {
                                        Text(
                                            "What's on your mind?",
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.secondary,
                                        unfocusedTextColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (editContent.isNotBlank()) {
                                            val updatedEntry = entry.copy(
                                                content = editContent,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                            mainViewModel.updateJournalEntry(updatedEntry)
                                            editingEntry = null
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Entry updated ✦")
                                            }
                                        }
                                    },
                                    enabled = editContent.isNotBlank(),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Changes", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    title = {
                        Text(
                            "Discard changes?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDiscardDialog = false
                                editingEntry = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Discard", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDiscardDialog = false }
                        ) {
                            Text("Keep Editing", color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        // Localized Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BoxDefaults.cardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = entry.dateDisplay,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.updatedAt != null) {
                        Text(
                            text = "(edited)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color(0xFFFFE7D0).copy(alpha = 0.35f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Edit Icon
                    val editInteractionSource = remember { MutableInteractionSource() }
                    val isEditPressed by editInteractionSource.collectIsPressedAsState()
                    val isEditHovered by editInteractionSource.collectIsHoveredAsState()
                    val editOpacity = if (isEditPressed || isEditHovered) 1f else 0.6f
                    IconButton(
                        onClick = onEditClick,
                        interactionSource = editInteractionSource,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = Color(0xFFFFE7D0).copy(alpha = editOpacity),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Trash Icon
                    val deleteInteractionSource = remember { MutableInteractionSource() }
                    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
                    val isDeleteHovered by deleteInteractionSource.collectIsHoveredAsState()
                    val deleteOpacity = if (isDeletePressed || isDeleteHovered) 1f else 0.6f
                    IconButton(
                        onClick = onDeleteClick,
                        interactionSource = deleteInteractionSource,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Delete Entry",
                            tint = Color(0xFFFFE7D0).copy(alpha = deleteOpacity),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

