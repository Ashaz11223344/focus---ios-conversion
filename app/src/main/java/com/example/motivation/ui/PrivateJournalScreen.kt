package com.example.motivation.ui

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.example.motivation.data.local.PrivateJournalEntry
import com.example.motivation.viewmodel.PrivateJournalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun android.content.Context.findActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateJournalScreen(
    viewModel: PrivateJournalViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf("") }
    val entries by viewModel.privateEntries.collectAsState()

    var showScreenshotOverlay by remember { mutableStateOf(false) }

    // 1. SecureScreen component for screenshot prevention & detection
    SecureScreen(
        onScreenshotAttempted = {
            showScreenshotOverlay = true
        }
    )

    // 2. Auto-Lock logic on Back / Screen Change
    DisposableEffect(Unit) {
        onDispose {
            viewModel.lock()
        }
    }

    // 3. Auto-Lock on App Minimization (ON_STOP)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.lock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
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
            // Secure Header: Title on the left, Orange Lock on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Private Journal",
                    fontFamily = com.example.motivation.ui.theme.EpilogueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secured Mode",
                    tint = Color(0xFFFC6E20), // Theme Orange
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onBackClick() }
                )
            }

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
                                "Write something private...",
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
                            if (noteText.isNotBlank()) {
                                viewModel.insertPrivateEntry(noteText)
                                noteText = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFC6E20),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "+ Add Entry",
                            fontFamily = com.example.motivation.ui.theme.EpilogueFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Text(
                text = "Secure Entires",
                fontFamily = com.example.motivation.ui.theme.EpilogueFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No private entries yet.",
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
                        PrivateJournalEntryCard(entry, onDelete = { viewModel.deletePrivateEntry(entry) })
                    }
                }
            }
        }

        if (showScreenshotOverlay) {
            AlertDialog(
                onDismissRequest = { showScreenshotOverlay = false },
                title = {
                    Text("Screenshot Blocked", fontWeight = FontWeight.Bold, color = Color(0xFFFC6E20))
                },
                text = {
                    Text("Nice try. Your journal likes its privacy...", color = MaterialTheme.colorScheme.onSurface)
                },
                confirmButton = {
                    Button(
                        onClick = { showScreenshotOverlay = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC6E20))
                    ) {
                        Text("Got it", color = Color.White)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
fun PrivateJournalEntryCard(
    entry: PrivateJournalEntry,
    onDelete: () -> Unit
) {
    val dateDisplay = remember(entry.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(entry.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFFFC6E20),
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.copyColorSecondary(),
                        modifier = Modifier.size(16.dp)
                    )
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${entry.wordCount} words ✦",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MaterialTheme.copyColorSecondary(): Color {
    return colorScheme.secondary.copy(alpha = 0.4f)
}
