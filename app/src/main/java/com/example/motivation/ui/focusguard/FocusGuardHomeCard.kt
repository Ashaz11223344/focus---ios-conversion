package com.example.motivation.ui.focusguard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.VibrantOrange

@Composable
fun FocusGuardHomeCard(
    isDndActive: Boolean,
    activeBlockedApps: List<String>,
    onNavigateToFocusGuard: () -> Unit
) {
    val isAnyActive = isDndActive || activeBlockedApps.isNotEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onNavigateToFocusGuard),
        shape = RoundedCornerShape(16.dp),
        color = if (isAnyActive) VibrantOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = if (isAnyActive)
            BorderStroke(1.dp, VibrantOrange.copy(alpha = 0.5f))
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (isAnyActive) VibrantOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Focus Guard",
                        style = TextStyle(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Active badge OR "Set up" label
                if (isAnyActive) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VibrantOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "● Active",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = TextStyle(
                                fontFamily = LiterataFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = VibrantOrange
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Set up →",
                        style = TextStyle(
                            fontFamily = LiterataFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Status rows (only shown when active)
            if (isAnyActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // DND status row
                if (isDndActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            tint = VibrantOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Notifications silenced",
                            style = TextStyle(
                                fontFamily = LiterataFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    if (activeBlockedApps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Blocked apps (show up to 3, then "+N more")
                if (activeBlockedApps.isNotEmpty()) {
                    val displayApps = activeBlockedApps.take(3)
                    val overflow = activeBlockedApps.size - 3

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = VibrantOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = buildString {
                                append(displayApps.joinToString(", "))
                                if (overflow > 0) append(" +$overflow more")
                                append(" blocked")
                            },
                            style = TextStyle(
                                fontFamily = LiterataFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

            } else {
                // Inactive state — two small chip-style labels
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniChip(icon = Icons.Rounded.NotificationsOff, label = "Silence hours")
                    MiniChip(icon = Icons.Rounded.Lock, label = "App blocker")
                }
            }
        }
    }
}

@Composable
private fun MiniChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = LiterataFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
