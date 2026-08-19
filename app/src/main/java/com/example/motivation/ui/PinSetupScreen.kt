package com.example.motivation.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motivation.security.PinManager
import com.example.motivation.ui.theme.LiterataFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    onSetupComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Design Colors (adapted dynamically to Light/Dark Mode)
    val darkBlack = MaterialTheme.colorScheme.background
    val creamBeige = MaterialTheme.colorScheme.onBackground
    val orange = Color(0xFFFC6E20)
    val charcoalGray = MaterialTheme.colorScheme.surfaceVariant

    // Auth States
    var isConfirmStep by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Spring Shake Animation
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 12f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            shakeOffset.animateTo(
                targetValue = -12f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            shakeOffset.animateTo(
                targetValue = 6f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            shakeOffset.animateTo(
                targetValue = -6f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
            )
            shakeOffset.animateTo(0f)
        }
    }

    fun handleInput(digit: Char) {
        if (currentInput.length >= 4) return
        val newInput = currentInput + digit
        currentInput = newInput

        if (newInput.length == 4) {
            coroutineScope.launch {
                delay(220) // Brief delay so the user sees the final dot fill up
                if (!isConfirmStep) {
                    firstPin = newInput
                    currentInput = ""
                    isConfirmStep = true
                    errorMessage = null
                } else {
                    if (newInput == firstPin) {
                        PinManager.setPin(context, newInput)
                        if (com.example.motivation.security.BiometricCapability.getAvailableStrength(context) != com.example.motivation.security.BiometricCapability.BiometricStrength.NONE) {
                            PinManager.setBiometricEnabled(context, true)
                        }
                        onSetupComplete()
                    } else {
                        errorMessage = "PINs don't match. Try again."
                        triggerShake()
                        currentInput = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlack)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onCancel) {
                    Text(
                        text = "← Back",
                        color = creamBeige.copy(alpha = 0.6f),
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Central Info Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = orange,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 12.dp)
                )

                Text(
                    text = if (isConfirmStep) "Confirm Your Private PIN" else "Create Your Private PIN",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = LiterataFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = creamBeige,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isConfirmStep) "Re-enter your 4-digit passcode to verify." else "This PIN protects your private journal. It is never stored in the cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = creamBeige.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Dots visualizer
                PinDots(
                    pinLength = currentInput.length,
                    shakeOffset = shakeOffset.value,
                    orange = orange,
                    cream = creamBeige
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Warning / Feedback Messages
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = orange,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Visual Number Keypad
            PinKeypad(
                onDigitClick = ::handleInput,
                onBackspaceClick = ::handleBackspace,
                orange = orange,
                cream = creamBeige,
                charcoal = charcoalGray
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PinDots(
    pinLength: Int,
    shakeOffset: Float,
    orange: Color,
    cream: Color
) {
    Row(
        modifier = Modifier
            .offset(x = shakeOffset.dp)
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val filled = i < pinLength
            val scale by animateFloatAsState(
                targetValue = if (filled) 1.25f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (filled) orange else Color.Transparent)
                    .border(
                        2.dp,
                        if (filled) orange else cream.copy(alpha = 0.35f),
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    orange: Color,
    cream: Color,
    charcoal: Color,
    onBiometricClick: (() -> Unit)? = null,
    biometricIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(null, '0', '⌫')
    )

    Column(
        modifier = Modifier
            .widthIn(max = 400.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.4f)
                    ) {
                        if (key != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(charcoal)
                                    .clickable {
                                        if (key == '⌫') onBackspaceClick()
                                        else onDigitClick(key)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == '⌫') {
                                    Icon(
                                        imageVector = Icons.Rounded.Backspace,
                                        contentDescription = "Backspace",
                                        tint = orange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = key.toString(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = LiterataFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = cream
                                    )
                                }
                            }
                        } else if (biometricIcon != null && onBiometricClick != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(charcoal)
                                    .clickable { onBiometricClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = biometricIcon,
                                    contentDescription = "Biometric unlock",
                                    tint = orange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
