package com.smartvault.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.smartvault.app.security.BiometricAuthManager
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*

@Composable
fun LockScreen(
    securityManager: SecurityManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }

    val biometricManager = remember { BiometricAuthManager(context) }
    val canUseBiometric = remember { biometricManager.canAuthenticate() && securityManager.isBiometricEnabled() }

    // Auto-prompt biometric on launch if enabled
    LaunchedEffect(Unit) {
        if (canUseBiometric && context is FragmentActivity) {
            biometricManager.promptBiometric(
                activity = context,
                onSuccess = {
                    securityManager.unlockApp()
                    onUnlocked()
                },
                onError = { /* fallback to PIN */ }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SmartVault Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your master PIN to access your vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = CrimsonError,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) IndigoPrimary else ObsidianSurfaceVariant)
                                .border(1.5.dp, if (isFilled) IndigoPrimary else ObsidianBorder, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showRecoveryDialog = true }) {
                    Text(
                        text = "Forgot PIN?",
                        color = IndigoLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Keypad
            Keypad(
                onDigitClick = { digit ->
                    errorMessage = null
                    if (pin.length < 4) {
                        pin += digit
                        if (pin.length == 4) {
                            if (securityManager.verifyPin(pin)) {
                                onUnlocked()
                            } else {
                                errorMessage = "Incorrect PIN. Try again."
                                pin = ""
                            }
                        }
                    }
                },
                onBackspace = {
                    errorMessage = null
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                },
                extraAction = {
                    if (canUseBiometric && context is FragmentActivity) {
                        IconButton(
                            onClick = {
                                biometricManager.promptBiometric(
                                    activity = context,
                                    onSuccess = {
                                        securityManager.unlockApp()
                                        onUnlocked()
                                    },
                                    onError = { err -> errorMessage = err }
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            )
        }
    }

    if (showRecoveryDialog) {
        val question = securityManager.getSecurityQuestion() ?: "Security question not set."
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Account Recovery", color = TextPrimaryDark) },
            text = {
                Column {
                    Text(
                        text = "Question: $question",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = { recoveryAnswer = it },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (securityManager.verifySecurityAnswer(recoveryAnswer)) {
                            Toast.makeText(context, "Recovery verified. Please set a new PIN.", Toast.LENGTH_LONG).show()
                            showRecoveryDialog = false
                            securityManager.unlockApp()
                            onUnlocked()
                        } else {
                            Toast.makeText(context, "Incorrect recovery answer.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Verify & Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}
