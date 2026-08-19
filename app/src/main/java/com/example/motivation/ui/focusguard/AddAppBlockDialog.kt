package com.example.motivation.ui.focusguard

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.motivation.R
import com.example.motivation.data.local.AppBlockRuleEntity
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.InterFontFamily
import com.example.motivation.ui.theme.VibrantOrange
import com.example.motivation.ui.BoxDefaults

@Composable
fun AddAppBlockDialog(
    packageName: String,
    appName: String,
    existing: AppBlockRuleEntity? = null,
    onSave: (AppBlockRuleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var startHour by remember { mutableStateOf(existing?.startHour ?: 9) }
    var startMinute by remember { mutableStateOf(existing?.startMinute ?: 0) }
    var endHour by remember { mutableStateOf(existing?.endHour ?: 17) }
    var endMinute by remember { mutableStateOf(existing?.endMinute ?: 0) }
    var daysOfWeek by remember { mutableStateOf(existing?.daysOfWeek ?: 127) } // Default all days

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (existing == null) stringResource(id = R.string.fg_block_app_title, appName) else stringResource(id = R.string.fg_edit_block_title, appName),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = stringResource(id = R.string.fg_block_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Start Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(context, { _, h, m ->
                                startHour = h
                                startMinute = m
                            }, startHour, startMinute, false).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.fg_start_time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = LiterataFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%02d:%02d", startHour, startMinute),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = LiterataFontFamily,
                            color = VibrantOrange
                        )
                    )
                }

                // End Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(context, { _, h, m ->
                                endHour = h
                                endMinute = m
                            }, endHour, endMinute, false).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(id = R.string.fg_end_time),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = LiterataFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%02d:%02d", endHour, endMinute),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = LiterataFontFamily,
                            color = VibrantOrange
                        )
                    )
                }

                // Day of week selector
                Text(
                    stringResource(id = R.string.fg_active_days),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LiterataFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { index, day ->
                        val isSelected = (daysOfWeek and (1 shl index)) != 0
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) VibrantOrange else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) VibrantOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    CircleShape
                                )
                                .clickable {
                                    daysOfWeek = if (isSelected) {
                                        daysOfWeek and (1 shl index).inv()
                                    } else {
                                        daysOfWeek or (1 shl index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(id = R.string.fg_cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = InterFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                AppBlockRuleEntity(
                                    packageName = packageName,
                                    appName = appName,
                                    startHour = startHour,
                                    startMinute = startMinute,
                                    endHour = endHour,
                                    endMinute = endMinute,
                                    daysOfWeek = daysOfWeek,
                                    isEnabled = existing?.isEnabled ?: true
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (existing == null) stringResource(id = R.string.fg_block_app) else stringResource(id = R.string.fg_save_changes),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }
    }
}
