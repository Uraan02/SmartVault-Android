package com.smartvault.app.ui.screens.radar

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
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.CurrencyFormatter
import com.smartvault.app.util.DateUtil
import com.smartvault.app.util.ExpiryUrgency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryRadarScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onCardClick: (Long) -> Unit
) {
    val cards by repository.getAllCards().collectAsState(initial = emptyList())
    val isPrivacyMode = securityManager.isPrivacyModeEnabled()

    val categorizedCards = remember(cards) {
        cards.map { card ->
            card to DateUtil.checkExpiryUrgency(card.expiryDate)
        }.sortedBy { it.second.ordinal }
    }

    val expiredList = remember(categorizedCards) { categorizedCards.filter { it.second == ExpiryUrgency.EXPIRED } }
    val critical30List = remember(categorizedCards) { categorizedCards.filter { it.second == ExpiryUrgency.CRITICAL_30_DAYS } }
    val warning60List = remember(categorizedCards) { categorizedCards.filter { it.second == ExpiryUrgency.WARNING_60_DAYS } }
    val notice90List = remember(categorizedCards) { categorizedCards.filter { it.second == ExpiryUrgency.NOTICE_90_DAYS } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Expiry Radar", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
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
                // Radar Summary Header
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(AmberWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Automatic Expiry Monitor", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text(
                                "Tracks bank cards, CNICs, licenses, and passports before they lapse.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // 1. Expired Cards
            if (expiredList.isNotEmpty()) {
                item {
                    Text("Expired Cards (${expiredList.size})", fontWeight = FontWeight.Bold, color = CrimsonError)
                }
                items(expiredList) { (card, _) ->
                    ExpiryCardItem(card = card, urgency = ExpiryUrgency.EXPIRED, isPrivacyMode = isPrivacyMode, onClick = { onCardClick(card.id) })
                }
            }

            // 2. Critical (< 30 days)
            if (critical30List.isNotEmpty()) {
                item {
                    Text("Expiring within 30 Days (${critical30List.size})", fontWeight = FontWeight.Bold, color = CrimsonError)
                }
                items(critical30List) { (card, _) ->
                    ExpiryCardItem(card = card, urgency = ExpiryUrgency.CRITICAL_30_DAYS, isPrivacyMode = isPrivacyMode, onClick = { onCardClick(card.id) })
                }
            }

            // 3. Warning (< 60 days)
            if (warning60List.isNotEmpty()) {
                item {
                    Text("Expiring within 60 Days (${warning60List.size})", fontWeight = FontWeight.Bold, color = AmberWarning)
                }
                items(warning60List) { (card, _) ->
                    ExpiryCardItem(card = card, urgency = ExpiryUrgency.WARNING_60_DAYS, isPrivacyMode = isPrivacyMode, onClick = { onCardClick(card.id) })
                }
            }

            // 4. Notice (< 90 days)
            if (notice90List.isNotEmpty()) {
                item {
                    Text("Expiring within 90 Days (${notice90List.size})", fontWeight = FontWeight.Bold, color = IndigoLight)
                }
                items(notice90List) { (card, _) ->
                    ExpiryCardItem(card = card, urgency = ExpiryUrgency.NOTICE_90_DAYS, isPrivacyMode = isPrivacyMode, onClick = { onCardClick(card.id) })
                }
            }

            if (expiredList.isEmpty() && critical30List.isEmpty() && warning60List.isEmpty() && notice90List.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("All Vault Cards are Active!", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("No cards expiring in the next 90 days.", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ExpiryCardItem(
    card: CardEntity,
    urgency: ExpiryUrgency,
    isPrivacyMode: Boolean,
    onClick: () -> Unit
) {
    val badgeColor = when (urgency) {
        ExpiryUrgency.EXPIRED -> CrimsonError
        ExpiryUrgency.CRITICAL_30_DAYS -> CrimsonError
        ExpiryUrgency.WARNING_60_DAYS -> AmberWarning
        ExpiryUrgency.NOTICE_90_DAYS -> IndigoLight
        else -> EmeraldAccent
    }
    val badgeText = when (urgency) {
        ExpiryUrgency.EXPIRED -> "EXPIRED"
        ExpiryUrgency.CRITICAL_30_DAYS -> "< 30 DAYS"
        ExpiryUrgency.WARNING_60_DAYS -> "< 60 DAYS"
        ExpiryUrgency.NOTICE_90_DAYS -> "< 90 DAYS"
        else -> "ACTIVE"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPrivacyMode) "••••••••" else card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${card.issuingBank ?: card.category} • Expiry: ${card.expiryDate ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
