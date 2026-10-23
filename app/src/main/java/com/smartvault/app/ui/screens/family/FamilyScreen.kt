package com.smartvault.app.ui.screens.family

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.smartvault.app.data.local.entity.FamilyMemberEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.SmartVaultBottomNavigation
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit,
    onMemberClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val familyMembers by repository.getAllFamilyMembers().collectAsState(initial = emptyList())
    val allCards by repository.getAllCards().collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var showAddDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newRelationship by remember { mutableStateOf("Spouse") }
    var newPhone by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = "Family Profiles",
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
                currentRoute = "family",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VioletPurple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VioletPurple.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletPurple.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VioletPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Family Card Wallet Hub", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(
                                "Manage distinct profiles and cards for each family member.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            items(familyMembers) { member ->
                val memberCards = allCards.filter { it.familyMemberId == member.id }
                val avatarColor = remember(member.avatarColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(member.avatarColorHex))
                    } catch (e: Exception) {
                        VioletPurple
                    }
                }

                Surface(
                    onClick = { onMemberClick(member.id) },
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPrivacyMode) "••••••••" else member.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ObsidianSurfaceVariant
                                ) {
                                    Text(
                                        text = member.relationship,
                                        color = IndigoLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${memberCards.size} cards assigned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Profile",
                            tint = TextSecondaryDark
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        val relationships = listOf("Self", "Spouse", "Child", "Parent", "Sibling", "Other")
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Family Member Profile", color = TextPrimaryDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Relationship", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationships.take(3).forEach { rel ->
                            FilterChip(
                                selected = newRelationship == rel,
                                onClick = { newRelationship = rel },
                                label = { Text(rel) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationships.drop(3).forEach { rel ->
                            FilterChip(
                                selected = newRelationship == rel,
                                onClick = { newRelationship = rel },
                                label = { Text(rel) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Notes") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank()) {
                            Toast.makeText(context, "Please enter a name.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val colors = listOf("#10B981", "#8B5CF6", "#EC4899", "#3B82F6", "#F59E0B")
                            val randomColor = colors.random()
                            repository.saveFamilyMember(
                                FamilyMemberEntity(
                                    name = newName.trim(),
                                    relationship = newRelationship,
                                    phone = newPhone.trim().ifBlank { null },
                                    notes = newNotes.trim().ifBlank { null },
                                    avatarColorHex = randomColor
                                )
                            )
                            newName = ""
                            newPhone = ""
                            newNotes = ""
                            showAddDialog = false
                            Toast.makeText(context, "Family profile created!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPurple)
                ) {
                    Text("Create Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}
