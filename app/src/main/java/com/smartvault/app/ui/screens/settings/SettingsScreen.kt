package com.smartvault.app.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.security.BiometricAuthManager
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricAuthManager(context) }
    val isBiometricSupported = remember { biometricManager.canAuthenticate() }

    var isBiometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var isScreenshotProtectionEnabled by remember { mutableStateOf(securityManager.isScreenshotProtectionEnabled()) }
    var selectedCurrency by remember { mutableStateOf(securityManager.getDefaultCurrency()) }
    var autoLockMs by remember { mutableStateOf(securityManager.getAutoLockTimeoutMs()) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Security & Settings", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(onClick = onLockApp) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", tint = CrimsonError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Security & App Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IndigoLight)
            }

            // Biometric Toggle
            item {
                SettingsSwitchRow(
                    title = "Fingerprint / Biometric Unlock",
                    subtitle = if (isBiometricSupported) "Unlock SmartVault using your fingerprint" else "Biometrics not available on this device",
                    icon = Icons.Default.Fingerprint,
                    iconColor = EmeraldAccent,
                    checked = isBiometricEnabled,
                    enabled = isBiometricSupported,
                    onCheckedChange = {
                        isBiometricEnabled = it
                        securityManager.setBiometricEnabled(it)
                    }
                )
            }

            // Screenshot Protection Toggle
            item {
                SettingsSwitchRow(
                    title = "Screenshot & Task Preview Protection",
                    subtitle = "Blocks screen capture and hides app preview in Android recent tasks",
                    icon = Icons.Default.Security,
                    iconColor = IndigoPrimary,
                    checked = isScreenshotProtectionEnabled,
                    onCheckedChange = {
                        isScreenshotProtectionEnabled = it
                        securityManager.setScreenshotProtectionEnabled(it)
                        Toast.makeText(context, if (it) "FLAG_SECURE enabled" else "FLAG_SECURE disabled", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Change PIN Button
            item {
                SettingsClickRow(
                    title = "Change Master PIN",
                    subtitle = "Update your 4-digit vault master access PIN",
                    icon = Icons.Default.Password,
                    iconColor = AmberWarning,
                    onClick = { showChangePinDialog = true }
                )
            }

            // Auto-lock Timeout
            item {
                SettingsClickRow(
                    title = "Auto-Lock Timeout",
                    subtitle = when (autoLockMs) {
                        0L -> "Immediately when closed"
                        30_000L -> "30 seconds"
                        60_000L -> "1 minute (Default)"
                        300_000L -> "5 minutes"
                        else -> "${autoLockMs / 60000} minutes"
                    },
                    icon = Icons.Default.Timer,
                    iconColor = CyanInfo,
                    onClick = {
                        val next = when (autoLockMs) {
                            0L -> 30_000L
                            30_000L -> 60_000L
                            60_000L -> 300_000L
                            else -> 0L
                        }
                        autoLockMs = next
                        securityManager.setAutoLockTimeoutMs(next)
                        Toast.makeText(context, "Auto-lock timeout updated.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Preferences
            item {
                Text("Preferences & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = IndigoLight)
            }

            // Currency
            item {
                SettingsClickRow(
                    title = "Default Vault Currency",
                    subtitle = "Current: $selectedCurrency (Pakistani Rupee)",
                    icon = Icons.Default.Paid,
                    iconColor = EmeraldAccent,
                    onClick = { showCurrencyDialog = true }
                )
            }

            // Backup & Restore
            item {
                SettingsClickRow(
                    title = "Encrypted Backup & Restore",
                    subtitle = "Export or restore password-encrypted .svault backup files",
                    icon = Icons.Default.Backup,
                    iconColor = VioletPurple,
                    onClick = onOpenBackupRestore
                )
            }

            // Privacy & Data Security Notice
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline & Private", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• All cards, photos, notes, and records are stored strictly on your local device.\n" +
                                    "• No accounts, servers, or cloud telemetry.\n" +
                                    "• CVVs and ATM PINs are never stored.\n" +
                                    "• Keep regular encrypted backups; deleting the app will erase local data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showCurrencyDialog) {
        val currencies = listOf("PKR", "USD", "EUR", "GBP", "AED", "SAR", "INR", "CAD")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Default Currency", color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currencies.forEach { cur ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCurrency == cur) ObsidianSurfaceVariant else Color.Transparent)
                                .padding(10.dp)
                        ) {
                            RadioButton(
                                selected = selectedCurrency == cur,
                                onClick = {
                                    selectedCurrency = cur
                                    securityManager.setDefaultCurrency(cur)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cur, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    if (showChangePinDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Master PIN", color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("Current PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("New PIN (4 digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!securityManager.verifyPin(oldPin)) {
                            Toast.makeText(context, "Current PIN is incorrect.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPin.length < 4 || newPin != confirmPin) {
                            Toast.makeText(context, "New PINs do not match or are too short.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        securityManager.setupPin(newPin)
                        Toast.makeText(context, "Master PIN updated successfully!", Toast.LENGTH_SHORT).show()
                        showChangePinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Update PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = IndigoPrimary,
                    checkedTrackColor = IndigoPrimary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun SettingsClickRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
        }
    }
}
