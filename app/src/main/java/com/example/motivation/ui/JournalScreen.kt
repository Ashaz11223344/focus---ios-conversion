package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.motivation.model.JournalEntry
import com.example.motivation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(mainViewModel: MainViewModel = viewModel()) {
    var noteText by remember { mutableStateOf("") }
    val entries by mainViewModel.journalEntries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    modifier = Modifier.align(Alignment.End).height(36.dp),
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
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(entries) { entry ->
                    JournalEntryCard(entry)
                }
            }
        }
    }
}

@Composable
fun JournalEntryCard(entry: JournalEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BoxDefaults.cardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = entry.dateDisplay,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
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
