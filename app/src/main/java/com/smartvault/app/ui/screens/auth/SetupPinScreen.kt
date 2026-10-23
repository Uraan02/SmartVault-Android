package com.smartvault.app.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*

@Composable
fun SetupPinScreen(
    securityManager: SecurityManager,
    onPinSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN, 3: Recovery Question
    var securityQuestion by remember { mutableStateOf("What was the name of your first school?") }
    var securityAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = when (step) {
                        1 -> "Create Master PIN"
                        2 -> "Confirm Master PIN"
                        else -> "Security Recovery"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (step) {
                        1 -> "Set a 4 or 6-digit PIN to securely protect your cards and records."
                        2 -> "Re-enter your PIN to verify."
                        else -> "Set an answer to recover access if you ever forget your PIN."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
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

                Spacer(modifier = Modifier.height(32.dp))

                if (step in 1..2) {
                    // PIN Dots Indicator
                    val currentInput = if (step == 1) pin else confirmPin
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < currentInput.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) IndigoPrimary else ObsidianSurfaceVariant)
                                    .border(1.5.dp, if (isFilled) IndigoPrimary else ObsidianBorder, CircleShape)
                            )
                        }
                    }
                } else {
                    // Recovery Question Input
                    OutlinedTextField(
                        value = securityAnswer,
                        onValueChange = { securityAnswer = it },
                        label = { Text("Security Answer") },
                        placeholder = { Text("e.g. Model High School") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedContainerColor = ObsidianSurface,
                            unfocusedContainerColor = ObsidianSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (securityAnswer.isBlank()) {
                                errorMessage = "Please enter an answer."
                            } else {
                                securityManager.setupPin(pin, securityQuestion, securityAnswer)
                                Toast.makeText(context, "SmartVault PIN configured successfully!", Toast.LENGTH_SHORT).show()
                                onPinSetupComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Complete Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            if (step in 1..2) {
                // Keypad
                Keypad(
                    onDigitClick = { digit ->
                        errorMessage = null
                        if (step == 1) {
                            if (pin.length < 4) {
                                pin += digit
                                if (pin.length == 4) {
                                    step = 2
                                }
                            }
                        } else {
                            if (confirmPin.length < 4) {
                                confirmPin += digit
                                if (confirmPin.length == 4) {
                                    if (confirmPin == pin) {
                                        step = 3 // Move to recovery question
                                    } else {
                                        errorMessage = "PINs do not match. Try again."
                                        confirmPin = ""
                                        step = 1
                                        pin = ""
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        errorMessage = null
                        if (step == 1) {
                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                        } else {
                            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
fun Keypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    extraAction: (@Composable () -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                for (digit in row) {
                    KeypadButton(text = digit, onClick = { onDigitClick(digit) })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (extraAction != null) {
                    extraAction()
                }
            }
            KeypadButton(text = "0", onClick = { onDigitClick("0") })
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = ObsidianSurfaceVariant,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
        }
    }
}
