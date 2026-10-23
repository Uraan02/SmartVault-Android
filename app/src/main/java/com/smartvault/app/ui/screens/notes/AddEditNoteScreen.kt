package com.smartvault.app.ui.screens.notes

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.local.entity.ChecklistItem
import com.smartvault.app.data.local.entity.NoteEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: Long? = null,
    cardId: Long? = null,
    contactId: Long? = null,
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allCards by repository.getAllCards().collectAsState(initial = emptyList())
    val familyMembers by repository.getAllFamilyMembers().collectAsState(initial = emptyList())

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isChecklist by remember { mutableStateOf(false) }
    var checklistItems by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var newChecklistText by remember { mutableStateOf("") }

    var isPinned by remember { mutableStateOf(false) }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderHoursAhead by remember { mutableStateOf(24) } // 24h default

    var selectedCardId by remember { mutableStateOf(cardId) }
    var selectedFamilyMemberId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0L) {
            repository.getAllNotes().collect { list ->
                val existing = list.find { it.id == noteId }
                existing?.let {
                    title = it.title
                    content = it.content
                    isChecklist = it.isChecklist
                    checklistItems = it.checklistItems
                    isPinned = it.isPinned
                    hasReminder = it.hasReminder
                    selectedCardId = it.cardId
                    selectedFamilyMemberId = it.familyMemberId
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId != null && noteId != 0L) "Edit Note" else "New Note", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) AmberWarning else TextSecondaryDark
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title *") },
                    placeholder = { Text("e.g. Card Renewal Checklist, Travel Vault") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Checklist Mode Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Checklist / To-Do Mode", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Switch(
                        checked = isChecklist,
                        onCheckedChange = { isChecklist = it }
                    )
                }
            }

            if (isChecklist) {
                item {
                    // Checklist Items
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        checklistItems.forEachIndexed { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { checked ->
                                        checklistItems = checklistItems.mapIndexed { idx, itm ->
                                            if (idx == index) itm.copy(isCompleted = checked) else itm
                                        }
                                    }
                                )
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimaryDark,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        checklistItems = checklistItems.filterIndexed { idx, _ -> idx != index }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Add new item input
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                placeholder = { Text("Add checklist item...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newChecklistText.isNotBlank()) {
                                        checklistItems = checklistItems + ChecklistItem(
                                            id = UUID.randomUUID().toString(),
                                            text = newChecklistText.trim(),
                                            isCompleted = false
                                        )
                                        newChecklistText = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Note Content") },
                        minLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Link to Card
            item {
                Text("Link to Card", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCardId == null,
                            onClick = { selectedCardId = null },
                            label = { Text("None") }
                        )
                    }
                    items(allCards) { card ->
                        FilterChip(
                            selected = selectedCardId == card.id,
                            onClick = { selectedCardId = card.id },
                            label = { Text(card.name) }
                        )
                    }
                }
            }

            // Reminder Options
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberWarning)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Schedule Reminder", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { hasReminder = it }
                            )
                        }

                        if (hasReminder) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Notify me in:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Spacer(modifier = Modifier.height(6.dp))
                            val options = listOf(1 to "1 Hour", 24 to "1 Day", 72 to "3 Days", 168 to "1 Week")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { (h, label) ->
                                    FilterChip(
                                        selected = reminderHoursAhead == h,
                                        onClick = { reminderHoursAhead = h },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save Note Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a note title.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val reminderTime = if (hasReminder) System.currentTimeMillis() + (reminderHoursAhead * 3600_000L) else null

                        coroutineScope.launch {
                            val note = NoteEntity(
                                id = noteId ?: 0L,
                                title = title.trim(),
                                content = content.trim(),
                                cardId = selectedCardId,
                                familyMemberId = selectedFamilyMemberId,
                                contactId = contactId,
                                isChecklist = isChecklist,
                                checklistItems = checklistItems,
                                isPinned = isPinned,
                                hasReminder = hasReminder,
                                reminderTime = reminderTime,
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.saveNote(note)
                            Toast.makeText(context, "Note saved to vault!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
