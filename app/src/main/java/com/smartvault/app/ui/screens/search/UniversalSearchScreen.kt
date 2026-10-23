package com.smartvault.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.EmptyVaultState
import com.smartvault.app.ui.screens.cards.CardListItem
import com.smartvault.app.ui.screens.expenses.TransactionRow
import com.smartvault.app.ui.screens.notes.NoteCard
import com.smartvault.app.ui.screens.visiting.VisitingContactRow
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onCardClick: (Long) -> Unit,
    onContactClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val isPrivacyMode = securityManager.isPrivacyModeEnabled()

    val matchedCards by repository.searchCards(searchQuery).collectAsState(initial = emptyList())
    val matchedContacts by repository.searchContacts(searchQuery).collectAsState(initial = emptyList())
    val matchedNotes by repository.searchNotes(searchQuery).collectAsState(initial = emptyList())
    val matchedTransactions by repository.searchTransactions(searchQuery).collectAsState(initial = emptyList())

    val totalMatches = remember(matchedCards, matchedContacts, matchedNotes, matchedTransactions) {
        matchedCards.size + matchedContacts.size + matchedNotes.size + matchedTransactions.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search cards, contacts, notes, expenses...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianSurface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Smart Universal Search", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text("Instantly search across your cards, contacts, notes, and records.", color = TextSecondaryDark)
                }
            }
        } else if (totalMatches == 0) {
            EmptyVaultState(
                title = "No results found",
                subtitle = "No vault records match '$searchQuery'."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Cards Results
                if (matchedCards.isNotEmpty()) {
                    item {
                        Text("Matched Cards (${matchedCards.size})", fontWeight = FontWeight.Bold, color = IndigoLight)
                    }
                    items(matchedCards) { card ->
                        CardListItem(
                            card = card,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { onCardClick(card.id) },
                            onToggleFavorite = {
                                coroutineScope.launch { repository.saveCard(card.copy(isFavorite = !card.isFavorite)) }
                            }
                        )
                    }
                }

                // Visiting Contacts Results
                if (matchedContacts.isNotEmpty()) {
                    item {
                        Text("Matched Visiting Contacts (${matchedContacts.size})", fontWeight = FontWeight.Bold, color = AmberWarning)
                    }
                    items(matchedContacts) { contact ->
                        VisitingContactRow(
                            contact = contact,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { onContactClick(contact.id) }
                        )
                    }
                }

                // Notes Results
                if (matchedNotes.isNotEmpty()) {
                    item {
                        Text("Matched Notes (${matchedNotes.size})", fontWeight = FontWeight.Bold, color = VioletPurple)
                    }
                    items(matchedNotes) { note ->
                        NoteCard(
                            note = note,
                            linkedCardName = null,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = {},
                            onDelete = {}
                        )
                    }
                }

                // Transactions Results
                if (matchedTransactions.isNotEmpty()) {
                    item {
                        Text("Matched Transactions (${matchedTransactions.size})", fontWeight = FontWeight.Bold, color = EmeraldAccent)
                    }
                    items(matchedTransactions) { tx ->
                        TransactionRow(
                            transaction = tx,
                            linkedCardName = null,
                            isPrivacyMode = isPrivacyMode,
                            onDelete = {}
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}
