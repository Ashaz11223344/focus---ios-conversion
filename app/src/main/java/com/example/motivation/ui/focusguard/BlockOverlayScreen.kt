package com.example.motivation.ui.focusguard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motivation.R
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.ui.theme.PlaywriteGBSFontFamily
import com.example.motivation.ui.theme.VibrantOrange

@Composable
fun BlockOverlayScreen(
    appName: String,
    packageName: String,
    untilTime: String,
    onGoBack: () -> Unit,
    onOverrideActive: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Shield Icon
            Icon(
                painter = painterResource(id = R.drawable.shield_locked_24),
                contentDescription = null,
                tint = VibrantOrange,
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(id = R.string.fg_app_blocked),
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = stringResource(id = R.string.fg_blocked_until, appName, untilTime),
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = stringResource(id = R.string.fg_block_quote),
                fontFamily = PlaywriteGBSFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 48.dp)
            )

            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.fg_go_back),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LiterataFontFamily,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onOverrideActive
            ) {
                Text(
                    text = stringResource(id = R.string.fg_override),
                    color = Color(0xFFF7F4EF).copy(alpha = 0.45f),
                    fontFamily = LiterataFontFamily,
                    fontSize = 14.sp
                )
            }
        }
    }
}

