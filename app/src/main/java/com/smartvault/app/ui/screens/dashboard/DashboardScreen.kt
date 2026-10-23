package com.smartvault.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.local.entity.ActivityLogEntity
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.MaskedCardNumber
import com.smartvault.app.ui.components.QuickCaptureModal
import com.smartvault.app.ui.components.SmartVaultBottomNavigation
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.components.EmptyVaultState
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.CurrencyFormatter
import com.smartvault.app.util.DateUtil
import com.smartvault.app.util.ExpiryUrgency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit,
    onCardClick: (Long) -> Unit
) {
    val cards by repository.getAllCards().collectAsState(initial = emptyList())
    val favoriteCards by repository.getFavoriteCards().collectAsState(initial = emptyList())
    val trackedCards by repository.getTrackedCards().collectAsState(initial = emptyList())
    val recentActivities by repository.getRecentActivities().collectAsState(initial = emptyList())
    val familyMembers by repository.getAllFamilyMembers().collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var isQuickCaptureOpen by remember { mutableStateOf(false) }

    // Compute expiry radar count
    val expiringCards = remember(cards) {
        cards.filter {
            val urgency = DateUtil.checkExpiryUrgency(it.expiryDate)
            urgency == ExpiryUrgency.EXPIRED ||
                    urgency == ExpiryUrgency.CRITICAL_30_DAYS ||
                    urgency == ExpiryUrgency.WARNING_60_DAYS ||
                    urgency == ExpiryUrgency.NOTICE_90_DAYS
        }
    }

    // Compute total tracked balance across all cards with tracking turned ON
    var totalTrackedBalance by remember { mutableStateOf(0.0) }
    var totalTrackedExpenses by remember { mutableStateOf(0.0) }

    LaunchedEffect(trackedCards) {
        var bal = 0.0
        var exp = 0.0
        for (c in trackedCards) {
            val summary = repository.getCardBalanceSummary(c)
            bal += summary.currentBalance
            exp += summary.totalExpenses
        }
        totalTrackedBalance = bal
        totalTrackedExpenses = exp
    }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = "SmartVault",
                isPrivacyMode = isPrivacyMode,
                onTogglePrivacy = {
                    isPrivacyMode = securityManager.togglePrivacyMode()
                },
                onLockApp = onLockApp,
                onOpenSearch = { onNavigate("universal_search") },
                onOpenExpiryRadar = { onNavigate("expiry_radar") },
                onOpenSettings = { onNavigate("settings") },
                expiringCount = expiringCards.size
            )
        },
        bottomBar = {
            SmartVaultBottomNavigation(
                currentRoute = "dashboard",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isQuickCaptureOpen = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Capture"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Expiry Radar Warning Banner
            if (expiringCards.isNotEmpty()) {
                item {
                    Surface(
                        onClick = { onNavigate("expiry_radar") },
                        shape = RoundedCornerShape(16.dp),
                        color = CrimsonError.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonError.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonError.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonError,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Card Expiry Radar Alert",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "${expiringCards.size} card(s) expiring soon or expired. Tap to view.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // 2. Tracked Balance Summary Widget (Only shows when balance tracking is enabled on cards!)
            if (trackedCards.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Total Tracked Balances",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${trackedCards.size} Cards Active",
                                        color = EmeraldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = CurrencyFormatter.format(totalTrackedBalance, securityManager.getDefaultCurrency(), isPrivacyMode),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalTrackedBalance >= 0) EmeraldAccent else CrimsonError
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Formula: Opening + Credits + Refunds − Expenses",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiaryDark
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = ObsidianBorder, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Recorded Expenses", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Text(
                                        text = CurrencyFormatter.format(totalTrackedExpenses, securityManager.getDefaultCurrency(), isPrivacyMode),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonError
                                    )
                                }
                                TextButton(onClick = { onNavigate("expenses") }) {
                                    Text("Manage Ledger", color = IndigoLight, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoLight)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Metrics Overview Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBox(
                        title = "Total Cards",
                        value = "${cards.size}",
                        icon = Icons.Default.CreditCard,
                        color = IndigoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("cards_list") }
                    )
                    MetricBox(
                        title = "Favourites",
                        value = "${favoriteCards.size}",
                        icon = Icons.Default.Star,
                        color = AmberWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("cards_list") }
                    )
                    MetricBox(
                        title = "Family",
                        value = "${familyMembers.size}",
                        icon = Icons.Default.Groups,
                        color = VioletPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("family") }
                    )
                }
            }

            // 4. Quick Scan Hero Banner
            item {
                Surface(
                    onClick = { onNavigate("camera_scan") },
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(IndigoPrimary.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = IndigoPrimary.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "SMART OCR SCANNER",
                                        color = IndigoLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Scan Any Card in Seconds",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Edge detection, automatic OCR, field detection & review",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recent Cards Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Cards",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    TextButton(onClick = { onNavigate("cards_list") }) {
                        Text("View All", color = IndigoLight)
                    }
                }
            }

            if (cards.isEmpty()) {
                item {
                    EmptyVaultState(
                        title = "No cards added yet",
                        subtitle = "Scan your first bank card, CNIC, or visiting card using the camera.",
                        actionText = "Scan Card",
                        onAction = { onNavigate("camera_scan") }
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cards.take(6)) { card ->
                            DashboardCardItem(
                                card = card,
                                isPrivacyMode = isPrivacyMode,
                                onClick = { onCardClick(card.id) }
                            )
                        }
                    }
                }
            }

            // 6. Recent Activity History Audit Log
            item {
                Text(
                    text = "Recent Activity Audit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            if (recentActivities.isEmpty()) {
                item {
                    Text(
                        text = "No recent activity recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }
            } else {
                items(recentActivities.take(5)) { log ->
                    ActivityLogRow(log = log)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    QuickCaptureModal(
        isOpen = isQuickCaptureOpen,
        onDismiss = { isQuickCaptureOpen = false },
        onScanCard = { onNavigate("camera_scan") },
        onAddCardManual = { onNavigate("add_edit_card") },
        onAddNote = { onNavigate("add_edit_note") },
        onAddExpense = { onNavigate("add_expense") }
    )
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimaryDark
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun DashboardCardItem(
    card: CardEntity,
    isPrivacyMode: Boolean,
    onClick: () -> Unit
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
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.issuingBank ?: card.category.replace("_", " "),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (card.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = AmberWarning,
                        modifier = Modifier.size(16.dp)
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
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.cardholderName ?: "CARDHOLDER",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = card.expiryDate ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ActivityLogRow(log: ActivityLogEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (log.action) {
                "CARD_CREATED" -> Icons.Default.CreditCard
                "CARD_UPDATED" -> Icons.Default.Edit
                "CARD_DELETED" -> Icons.Default.Delete
                "TRANSACTION_ADDED" -> Icons.Default.ReceiptLong
                "BACKUP_RESTORED" -> Icons.Default.Restore
                else -> Icons.Default.Info
            }
            val iconColor = when (log.action) {
                "CARD_CREATED" -> IndigoPrimary
                "TRANSACTION_ADDED" -> EmeraldAccent
                "CARD_DELETED" -> CrimsonError
                else -> AmberWarning
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark
                )
                Text(
                    text = DateUtil.formatDateTime(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiaryDark
                )
            }
        }
    }
}
