package com.example.motivation.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import com.example.motivation.ui.theme.PlaywriteGBSFontFamily

@Composable
fun QuoteScreen(navController: NavController, userName: String, mainViewModel: MainViewModel = viewModel()) {
    val quote by mainViewModel.quote.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val isFavorite = remember(quote, favorites) { favorites.contains(quote) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTabletOrFoldable = screenWidthDp > 600

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            // Personalized Greeting
            Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Quote Card
            val quoteFontSize = if (isTabletOrFoldable) 42.sp else 35.sp
            val quoteLineHeight = if (isTabletOrFoldable) 56.sp else 48.sp
            val cardMinHeight = if (isTabletOrFoldable) 300.dp else 250.dp

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = cardMinHeight),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BoxDefaults.cardBorder()
            ) {
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
                    quote?.let { shareQuote(context, it.text) }
                }
                ActionItem(icon = Icons.Default.ContentCopy, label = "Copy") {
                    quote?.let { clipboardManager.setText(AnnotatedString(it.text)) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                ExploreItemData("Favorites", "favorites", Icons.Default.FavoriteBorder),
                ExploreItemData("History", "history", Icons.Default.History),
                ExploreItemData("Search", "search", Icons.Default.Search),
                ExploreItemData("Mood", "mood", Icons.Default.SentimentSatisfiedAlt),
                ExploreItemData("Journal", "journal", Icons.Default.Book),
                ExploreItemData("Settings", "settings", Icons.Default.Settings)
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
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

data class ExploreItemData(val title: String, val route: String, val icon: ImageVector)

fun shareQuote(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
