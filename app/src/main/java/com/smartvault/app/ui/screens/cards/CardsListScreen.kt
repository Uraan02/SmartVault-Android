package com.smartvault.app.ui.screens.cards

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.EmptyVaultState
import com.smartvault.app.ui.components.SmartVaultBottomNavigation
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsListScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit,
    onCardClick: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allCards by repository.getAllCards().collectAsState(initial = emptyList())
    val categories by repository.getAllCategories().collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null = All
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, FAVORITES, ARCHIVED
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = remember(allCards, selectedCategory, selectedFilter, searchQuery) {
        allCards.filter { card ->
            val matchesCategory = if (selectedCategory == null) true else card.category == selectedCategory
            val matchesFilter = when (selectedFilter) {
                "FAVORITES" -> card.isFavorite && !card.isArchived
                "ARCHIVED" -> card.isArchived
                else -> !card.isArchived
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                card.name.contains(searchQuery, ignoreCase = true) ||
                        (card.cardholderName?.contains(searchQuery, ignoreCase = true) == true) ||
                        (card.issuingBank?.contains(searchQuery, ignoreCase = true) == true) ||
                        (card.cardNumber?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesCategory && matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = "Card Vault",
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
                currentRoute = "cards_list",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate("camera_scan") },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Grid/List Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search cards by name, bank, digits...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryDark)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = IndigoLight
                    )
                }
            }

            // Category Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else {
                    val idx = categories.indexOfFirst { it.name == selectedCategory }
                    if (idx >= 0) idx + 1 else 0
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = IndigoPrimary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    text = {
                        Text(
                            text = "All Cards (${allCards.size})",
                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategory == null) IndigoPrimary else TextSecondaryDark
                        )
                    }
                )
                val standardCats = listOf(
                    "PERSONAL" to "Personal",
                    "BANK_PAYMENT" to "Bank & Payment",
                    "FAMILY" to "Family",
                    "VISITING" to "Visiting",
                    "IDENTITY_MEMBERSHIP" to "ID & Member"
                )
                standardCats.forEach { (catKey, catLabel) ->
                    val isSel = selectedCategory == catKey
                    val count = allCards.count { it.category == catKey && !it.isArchived }
                    Tab(
                        selected = isSel,
                        onClick = { selectedCategory = catKey },
                        text = {
                            Text(
                                text = "$catLabel ($count)",
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) IndigoPrimary else TextSecondaryDark
                            )
                        }
                    )
                }
            }

            // Quick Filter Chips (All, Favorites, Archived)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Active") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ObsidianSurfaceVariant,
                        selectedLabelColor = IndigoPrimary
                    )
                )
                FilterChip(
                    selected = selectedFilter == "FAVORITES",
                    onClick = { selectedFilter = "FAVORITES" },
                    label = { Text("Favourites ★") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberWarning.copy(alpha = 0.2f),
                        selectedLabelColor = AmberWarning
                    )
                )
                FilterChip(
                    selected = selectedFilter == "ARCHIVED",
                    onClick = { selectedFilter = "ARCHIVED" },
                    label = { Text("Archived") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ObsidianSurfaceVariant,
                        selectedLabelColor = TextSecondaryDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cards Content
            if (filteredCards.isEmpty()) {
                EmptyVaultState(
                    title = "No cards found",
                    subtitle = if (searchQuery.isNotEmpty()) "No cards match '$searchQuery'." else "No cards in this category yet.",
                    actionText = "Scan Card",
                    onAction = { onNavigate("camera_scan") }
                )
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCards) { card ->
                        CardGridItem(
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
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCards) { card ->
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
            }
        }
    }
}

@Composable
fun CardGridItem(
    card: CardEntity,
    isPrivacyMode: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val cardColor = remember(card.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.colorHex))
        } catch (e: Exception) {
            CardBlue
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.issuingBank ?: card.category.take(12),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (card.isFavorite) AmberWarning else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = if (isPrivacyMode) "••••••••" else card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.maskCardNumber(card.cardNumber, isPrivacyMode),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (card.isTrackingEnabled) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldDark.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "Tracked",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = card.expiryDate ?: "",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CardListItem(
    card: CardEntity,
    isPrivacyMode: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val cardColor = remember(card.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.colorHex))
        } catch (e: Exception) {
            CardBlue
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPrivacyMode) "••••••••" else card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (card.isTrackingEnabled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Tracked",
                                color = EmeraldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.maskCardNumber(card.cardNumber, isPrivacyMode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    letterSpacing = 0.8.sp
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (card.isFavorite) AmberWarning else TextSecondaryDark
                )
            }
        }
    }
}
