package com.smartvault.app.ui.screens.family

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.screens.cards.CardListItem
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyProfileScreen(
    memberId: Long,
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onCardClick: (Long) -> Unit,
    onAddCard: (Long) -> Unit,
    onLockApp: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val memberState by repository.getFamilyMemberById(memberId).collectAsState(initial = null)
    val cards by repository.getCardsByFamilyMember(memberId).collectAsState(initial = emptyList())
    val notes by repository.getNotesByFamilyMember(memberId).collectAsState(initial = emptyList())
    val transactions by repository.getTransactionsByFamilyMember(memberId).collectAsState(initial = emptyList())

    val isPrivacyMode = securityManager.isPrivacyModeEnabled()
    val member = memberState

    if (member == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VioletPurple)
        }
        return
    }

    val avatarColor = remember(member.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(member.avatarColorHex))
        } catch (e: Exception) {
            VioletPurple
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(member.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
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
                // Profile Header Card
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
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPrivacyMode) "••••••••" else member.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VioletPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = member.relationship,
                                color = VioletPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (!member.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = member.phone!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }

                        if (!member.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = member.notes!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiaryDark
                            )
                        }
                    }
                }
            }

            // Member Cards Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cards Belonging to ${member.name} (${cards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    TextButton(onClick = { onAddCard(member.id) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Card", color = IndigoLight)
                    }
                }
            }

            if (cards.isEmpty()) {
                item {
                    Text(
                        text = "No cards assigned to this profile yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }
            } else {
                items(cards) { card ->
                    CardListItem(
                        card = card,
                        isPrivacyMode = isPrivacyMode,
                        onClick = { onCardClick(card.id) },
                        onToggleFavorite = {
                            coroutineScope.launch {
                                repository.saveCard(card.copy(isFavorite = !card.isFavorite))
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
