package com.smartvault.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var confirmBackupPassword by remember { mutableStateOf("") }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restorePassword by remember { mutableStateOf("") }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // File picker for restore
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            showRestoreDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encrypted Backup & Recovery", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
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
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VioletPurple.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletPurple.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = VioletPurple, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("AES-256-GCM Military Encryption", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(
                                "Your backups are encrypted with your chosen master password. Nobody can read your vault data without this password.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Export Backup Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Export Encrypted Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Creates an encrypted .svault package containing your cards, family records, expenses, notes, and visiting contacts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateBackupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Encrypted Backup (.svault)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Restore Backup Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Restore from Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select a previously exported .svault backup file to restore your vault data after changing phones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { restoreFilePicker.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = IndigoLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Backup File to Restore", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    // Dialog for creating backup
    if (showCreateBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateBackupDialog = false },
            title = { Text("Set Backup Password", color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a password to encrypt this backup file.", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmBackupPassword,
                        onValueChange = { confirmBackupPassword = it },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.isBlank() || backupPassword != confirmBackupPassword) {
                            Toast.makeText(context, "Passwords do not match or are blank.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            try {
                                val encryptedBytes = repository.createEncryptedBackup(backupPassword.toCharArray())
                                val fileName = "SmartVault_Backup_${System.currentTimeMillis()}.svault"
                                val cacheFile = File(context.cacheDir, fileName)
                                cacheFile.writeBytes(encryptedBytes)

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                                    putExtra(Intent.EXTRA_TEXT, "Encrypted SmartVault Backup")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Save Backup File"))
                                Toast.makeText(context, "Encrypted backup file created successfully!", Toast.LENGTH_LONG).show()
                                showCreateBackupDialog = false
                                backupPassword = ""
                                confirmBackupPassword = ""
                            } catch (e: Exception) {
                                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPurple)
                ) {
                    Text("Encrypt & Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBackupDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }

    // Dialog for restoring backup
    if (showRestoreDialog && selectedRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Enter Backup Password", color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the password that was used when creating this backup.", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restorePassword.isBlank()) {
                            Toast.makeText(context, "Please enter the password.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            try {
                                val inputStream = context.contentResolver.openInputStream(selectedRestoreUri!!)
                                val bytes = inputStream?.readBytes() ?: throw IllegalArgumentException("Could not read file.")
                                inputStream.close()

                                val count = repository.restoreEncryptedBackup(bytes, restorePassword.toCharArray())
                                Toast.makeText(context, "Successfully restored $count cards and records!", Toast.LENGTH_LONG).show()
                                showRestoreDialog = false
                                restorePassword = ""
                                onNavigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Decryption error: Incorrect password or corrupted file.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPurple)
                ) {
                    Text("Decrypt & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}
