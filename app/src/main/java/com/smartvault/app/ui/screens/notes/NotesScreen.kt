package com.smartvault.app.ui.screens.notes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.local.entity.NoteEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.EmptyVaultState
import com.smartvault.app.ui.components.SmartVaultBottomNavigation
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.DateUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit,
    onNoteClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allNotes by repository.getAllNotes().collectAsState(initial = emptyList())
    val allCards by repository.getAllCards().collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, PINNED, CHECKLISTS, REMINDERS

    val filteredNotes = remember(allNotes, searchQuery, filterType) {
        allNotes.filter { n ->
            val matchesFilter = when (filterType) {
                "PINNED" -> n.isPinned
                "CHECKLISTS" -> n.isChecklist
                "REMINDERS" -> n.hasReminder
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                n.title.contains(searchQuery, ignoreCase = true) ||
                        n.content.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = "Notes & Reminders",
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
                currentRoute = "notes",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate("add_edit_note") },
                containerColor = AmberWarning,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes, checklists, reminders...") },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("All (${allNotes.size})") }
                )
                FilterChip(
                    selected = filterType == "PINNED",
                    onClick = { filterType = "PINNED" },
                    label = { Text("Pinned 📌") }
                )
                FilterChip(
                    selected = filterType == "CHECKLISTS",
                    onClick = { filterType = "CHECKLISTS" },
                    label = { Text("Checklists ✓") }
                )
                FilterChip(
                    selected = filterType == "REMINDERS",
                    onClick = { filterType = "REMINDERS" },
                    label = { Text("Reminders ⏰") }
                )
            }

            // Notes List
            if (filteredNotes.isEmpty()) {
                EmptyVaultState(
                    title = "No notes found",
                    subtitle = "Jot down confidential notes, PIN recovery hints, card renewal checklists, or follow-ups.",
                    actionText = "Create Note",
                    onAction = { onNavigate("add_edit_note") }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes) { note ->
                        val linkedCard = allCards.find { it.id == note.cardId }
                        NoteCard(
                            note = note,
                            linkedCardName = linkedCard?.name,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = {
                                coroutineScope.launch {
                                    repository.saveNote(note.copy(isPinned = !note.isPinned))
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    repository.deleteNote(note)
                                    Toast.makeText(context, "Note deleted.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    linkedCardName: String?,
    isPrivacyMode: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (note.isPinned) AmberWarning.copy(alpha = 0.5f) else ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPrivacyMode) "••••••••" else note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (note.isPinned) AmberWarning else TextTertiaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextTertiaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    note.checklistItems.take(3).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.isCompleted) EmeraldAccent else TextSecondaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (item.isCompleted) TextTertiaryDark else TextPrimaryDark,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                maxLines = 1
                            )
                        }
                    }
                    if (note.checklistItems.size > 3) {
                        Text(
                            text = "+ ${note.checklistItems.size - 3} more items",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoLight
                        )
                    }
                }
            } else {
                Text(
                    text = if (isPrivacyMode) "••••••••••••••••••••" else note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtil.formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiaryDark
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (note.hasReminder && note.reminderTime != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = DateUtil.formatDateTime(note.reminderTime!!),
                                    color = AmberWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (linkedCardName != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndigoPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = linkedCardName,
                                color = IndigoLight,
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
