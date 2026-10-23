package com.smartvault.app.ui.screens.visiting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.smartvault.app.data.local.entity.VisitingContactEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.screens.cards.DetailRow
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.DateUtil
import com.smartvault.app.util.VcfExporter
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contactState by repository.getVisitingContactById(contactId).collectAsState(initial = null)
    val linkedNotes by repository.getNotesByContact(contactId).collectAsState(initial = emptyList())

    val isPrivacyMode = securityManager.isPrivacyModeEnabled()
    val contact = contactState

    var showMeetingNotesDialog by remember { mutableStateOf(false) }
    var meetingNotesText by remember { mutableStateOf("") }

    if (contact == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AmberWarning)
        }
        return
    }

    LaunchedEffect(contact) {
        meetingNotesText = contact.meetingNotes ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val vcf = VcfExporter.exportContact(contact)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/x-vcard"
                                putExtra(Intent.EXTRA_SUBJECT, "${contact.name}.vcf")
                                putExtra(Intent.EXTRA_TEXT, vcf)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share vCard"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AmberWarning)
                    }
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
                // Contact Card Header
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AmberWarning),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPrivacyMode) "••••••••" else contact.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (!contact.designation.isNullOrBlank()) {
                            Text(
                                text = contact.designation!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                        if (!contact.company.isNullOrBlank()) {
                            Text(
                                text = contact.company!!,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberWarning
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Quick Action Intents: Call, Email, Website, Map
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (!contact.phone.isNullOrBlank()) {
                                ActionCircleButton(icon = Icons.Default.Phone, label = "Call", color = EmeraldAccent) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                    context.startActivity(intent)
                                }
                            }
                            if (!contact.email.isNullOrBlank()) {
                                ActionCircleButton(icon = Icons.Default.Email, label = "Email", color = IndigoPrimary) {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                                    context.startActivity(intent)
                                }
                            }
                            if (!contact.website.isNullOrBlank()) {
                                ActionCircleButton(icon = Icons.Default.Language, label = "Website", color = CyanInfo) {
                                    var url = contact.website!!
                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        url = "https://$url"
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                            if (!contact.address.isNullOrBlank()) {
                                ActionCircleButton(icon = Icons.Default.LocationOn, label = "Map", color = RosePink) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(contact.address)}"))
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            }

            // Original Card Photo
            if (!contact.frontImagePath.isNullOrBlank()) {
                item {
                    Text("Original Visiting Card", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    AsyncImage(
                        model = File(contact.frontImagePath!!),
                        contentDescription = "Visiting Card Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianSurface)
                    )
                }
            }

            // Meeting Notes & Follow-up Section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Meeting Notes & Follow-Up", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            IconButton(onClick = { showMeetingNotesDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Notes", tint = AmberWarning)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (contact.meetingNotes.isNullOrBlank()) "No meeting notes recorded yet. Tap edit to log discussion points or follow-up tasks." else contact.meetingNotes!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (contact.meetingNotes.isNullOrBlank()) TextSecondaryDark else TextPrimaryDark
                        )
                    }
                }
            }

            // Contact Info Details
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Contact Information", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!contact.phone.isNullOrBlank()) DetailRow("Phone", contact.phone!!)
                        if (!contact.email.isNullOrBlank()) DetailRow("Email", contact.email!!)
                        if (!contact.website.isNullOrBlank()) DetailRow("Website", contact.website!!)
                        if (!contact.address.isNullOrBlank()) DetailRow("Address", contact.address!!)
                        DetailRow("Created Date", DateUtil.formatDate(contact.createdAt))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showMeetingNotesDialog) {
        AlertDialog(
            onDismissRequest = { showMeetingNotesDialog = false },
            title = { Text("Log Meeting Notes & Follow-up", color = TextPrimaryDark) },
            text = {
                OutlinedTextField(
                    value = meetingNotesText,
                    onValueChange = { meetingNotesText = it },
                    label = { Text("Meeting notes, discussion items...") },
                    minLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.saveVisitingContact(contact.copy(meetingNotes = meetingNotesText.trim()))
                            Toast.makeText(context, "Meeting notes updated!", Toast.LENGTH_SHORT).show()
                            showMeetingNotesDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                ) {
                    Text("Save Notes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMeetingNotesDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
fun ActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
    }
}
