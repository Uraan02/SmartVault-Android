package com.smartvault.app.ui.screens.visiting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.local.entity.VisitingContactEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.EmptyVaultState
import com.smartvault.app.ui.components.SmartVaultBottomNavigation
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.VcfExporter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitingCardsScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit,
    onContactClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contacts by repository.getAllVisitingContacts().collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var selectedTag by remember { mutableStateOf<String?>(null) } // null = All
    var searchQuery by remember { mutableStateOf("") }

    val tags = listOf("Client", "Supplier", "Lead", "Partner", "Friend")

    val filteredContacts = remember(contacts, selectedTag, searchQuery) {
        contacts.filter { c ->
            val matchesTag = if (selectedTag == null) true else c.tags.contains(selectedTag)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                c.name.contains(searchQuery, ignoreCase = true) ||
                        (c.company?.contains(searchQuery, ignoreCase = true) == true) ||
                        (c.phone?.contains(searchQuery, ignoreCase = true) == true) ||
                        (c.designation?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesTag && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = "Visiting Directory",
                isPrivacyMode = isPrivacyMode,
                onTogglePrivacy = {
                    isPrivacyMode = securityManager.togglePrivacyMode()
                },
                onLockApp = onLockApp,
                onOpenSearch = { onNavigate("universal_search") },
                onOpenExpiryRadar = { onNavigate("expiry_radar") },
                onOpenSettings = { onNavigate("settings") }
            )
        },
        bottomBar = {
            SmartVaultBottomNavigation(
                currentRoute = "visiting_cards",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate("camera_scan") },
                containerColor = AmberWarning,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Visiting Card")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Export vCard button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, company, designation...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedBorderColor = AmberWarning,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = {
                        if (contacts.isEmpty()) {
                            Toast.makeText(context, "No contacts to export.", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val vcfText = VcfExporter.exportContacts(contacts)
                        val cacheFile = File(context.cacheDir, "visiting_cards_export.vcf")
                        cacheFile.writeText(vcfText)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/x-vcard"
                            putExtra(Intent.EXTRA_SUBJECT, "SmartVault Visiting Contacts.vcf")
                            putExtra(Intent.EXTRA_TEXT, vcfText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Contacts (.vcf)"))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export VCF",
                        tint = AmberWarning
                    )
                }
            }

            // Tag Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("All (${contacts.size})") }
                    )
                }
                items(tags) { tag ->
                    val count = contacts.count { it.tags.contains(tag) }
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        label = { Text("$tag ($count)") }
                    )
                }
            }

            // Contacts List
            if (filteredContacts.isEmpty()) {
                EmptyVaultState(
                    title = "No visiting cards found",
                    subtitle = "Scan paper business cards to extract contacts and auto-dial/email.",
                    actionText = "Scan Business Card",
                    onAction = { onNavigate("camera_scan") }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredContacts) { contact ->
                        VisitingContactRow(
                            contact = contact,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { onContactClick(contact.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }
}

@Composable
fun VisitingContactRow(
    contact: VisitingContactEntity,
    isPrivacyMode: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AmberWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isPrivacyMode) "••••••••" else contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!contact.designation.isNullOrBlank() || !contact.company.isNullOrBlank()) {
                            Text(
                                text = listOfNotNull(contact.designation, contact.company).joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action Call & Email Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!contact.phone.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (!contact.email.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IndigoPrimary.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = IndigoLight, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (contact.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    contact.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ObsidianSurfaceVariant
                        ) {
                            Text(
                                text = tag,
                                color = AmberWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
