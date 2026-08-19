package com.example.motivation.ui

import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motivation.MainActivity
import com.example.motivation.security.BiometricCapability
import com.example.motivation.security.PinManager
import com.example.motivation.ui.BoxDefaults
import com.example.motivation.ui.theme.LiterataFontFamily
import com.example.motivation.viewmodel.PrivateJournalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.findMainActivity(): MainActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is MainActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun PinUnlockScreen(
    viewModel: PrivateJournalViewModel,
    onUnlockComplete: () -> Unit,
    onResetPin: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Design Colors (adapted dynamically to Light/Dark Mode)
    val darkBlack = MaterialTheme.colorScheme.background
    val creamBeige = MaterialTheme.colorScheme.onBackground
    val orange = Color(0xFFFC6E20)
    val charcoalGray = MaterialTheme.colorScheme.surfaceVariant

    // State Variables
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetWarning by remember { mutableStateOf(false) }

    val lockoutSecs by viewModel.lockoutTimeRemainingSecs.collectAsState()

    // Spring Shake Offset
    val shakeOffset = remember { Animatable(0f) }

    val biometricType = remember { BiometricCapability.getAvailableBiometricType(context) }
    val isBiometricAvailable = biometricType != BiometricCapability.BiometricType.NONE
    val isBiometricEnabled = remember { PinManager.isBiometricEnabled(context) }

    val activity = remember(context) { context.findMainActivity() }

    android.util.Log.d("PinUnlockScreen", "biometricType=$biometricType, available=$isBiometricAvailable, enabled=$isBiometricEnabled, activity=${activity != null}")

    // Auto-trigger biometric prompt on startup if enabled
    LaunchedEffect(Unit) {
        if (isBiometricAvailable && isBiometricEnabled && activity != null) {
            activity.triggerBiometricPrompt(
                onSuccess = {
                    viewModel.unlock()
                    onUnlockComplete()
                },
                onError = { error ->
                    errorMessage = error
                }
            )
        }
    }

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
        if (viewModel.isLockedOut()) return
        if (currentInput.length >= 4) return
        val newInput = currentInput + digit
        currentInput = newInput

        if (newInput.length == 4) {
            coroutineScope.launch {
                delay(220) // Brief delay so user sees last dot update
                if (PinManager.verifyPin(context, newInput)) {
                    viewModel.unlock()
                    currentInput = ""
                    onUnlockComplete()
                } else {
                    viewModel.recordFailedAttempt()
                    if (viewModel.isLockedOut()) {
                        errorMessage = "Too many attempts. Lockout active."
                    } else {
                        errorMessage = "Incorrect PIN. Attempts: ${viewModel.getFailedAttempts()}/5"
                    }
                    triggerShake()
                    currentInput = ""
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

            // Central Content Frame
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (isBiometricAvailable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BoxDefaults.cardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (biometricType) {
                                    BiometricCapability.BiometricType.FINGERPRINT -> Icons.Rounded.Fingerprint
                                    BiometricCapability.BiometricType.FACE -> Icons.Rounded.Face
                                    BiometricCapability.BiometricType.FINGERPRINT_AND_FACE -> Icons.Rounded.Fingerprint
                                    else -> Icons.Rounded.Lock
                                },
                                contentDescription = "Biometric unlock",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = when (biometricType) {
                                    BiometricCapability.BiometricType.FINGERPRINT -> "Unlock with Fingerprint"
                                    BiometricCapability.BiometricType.FACE -> "Unlock with Face"
                                    BiometricCapability.BiometricType.FINGERPRINT_AND_FACE -> "Unlock with Biometric"
                                    else -> "Unlock Biometric"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    activity?.triggerBiometricPrompt(
                                        onSuccess = {
                                            viewModel.unlock()
                                            onUnlockComplete()
                                        },
                                        onError = { error ->
                                            errorMessage = error
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Try Biometric", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = orange,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Unlock Private Journal",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = creamBeige,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (lockoutSecs > 0) "Locked out for safety. Please wait." else "Enter your 4-digit PIN to decrypt your journals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = creamBeige.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

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
                if (lockoutSecs > 0) {
                    Text(
                        text = "Too many attempts. Try again in ${lockoutSecs}s.",
                        color = orange,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                } else {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = orange,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Visual Number Keypad
            val biometricIcon = if (isBiometricAvailable) {
                when (biometricType) {
                    BiometricCapability.BiometricType.FINGERPRINT -> Icons.Rounded.Fingerprint
                    BiometricCapability.BiometricType.FACE -> Icons.Rounded.Face
                    BiometricCapability.BiometricType.FINGERPRINT_AND_FACE -> Icons.Rounded.Fingerprint
                    else -> null
                }
            } else {
                null
            }

            PinKeypad(
                onDigitClick = ::handleInput,
                onBackspaceClick = ::handleBackspace,
                orange = orange,
                cream = creamBeige,
                charcoal = charcoalGray,
                onBiometricClick = {
                    activity?.triggerBiometricPrompt(
                        onSuccess = {
                            viewModel.unlock()
                            onUnlockComplete()
                        },
                        onError = { error ->
                            errorMessage = error
                        }
                    )
                },
                biometricIcon = biometricIcon
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Forgot PIN? action text
            Text(
                text = "Forgot PIN?",
                color = creamBeige.copy(alpha = 0.5f),
                fontFamily = LiterataFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { showResetWarning = true }
                    .padding(8.dp)
            )
        }

        // Forgot PIN deletion warning dialog overlay
        if (showResetWarning) {
            AlertDialog(
                onDismissRequest = { showResetWarning = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = orange,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Wipe Private Journal?",
                            fontFamily = LiterataFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = orange
                        )
                    }
                },
                text = {
                    Text(
                        text = "For security, PIN reset is not supported without erasing data. This will permanently delete all private journal entries. Are you sure?",
                        fontFamily = LiterataFontFamily,
                        color = creamBeige.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetWarning = false
                            viewModel.resetAndDeleteAllPrivateData(context)
                            onResetPin()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = orange)
                    ) {
                        Text("Reset & Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showResetWarning = false },
                        border = BorderStroke(1.dp, creamBeige.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = creamBeige)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = charcoalGray,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}
