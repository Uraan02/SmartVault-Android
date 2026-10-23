package com.smartvault.app.ui.screens.cards

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.local.entity.TransactionEntity
import com.smartvault.app.data.repository.CardBalanceSummary
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.MaskedCardNumber
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.CurrencyFormatter
import com.smartvault.app.util.DateUtil
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onEditCard: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onAddNote: (Long) -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cardState by repository.getCardById(cardId).collectAsState(initial = null)
    val transactions by repository.getTransactionsByCard(cardId).collectAsState(initial = emptyList())
    val linkedNotes by repository.getNotesByCard(cardId).collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var balanceSummary by remember {
        mutableStateOf(CardBalanceSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0))
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cardState, transactions) {
        cardState?.let {
            if (it.isTrackingEnabled) {
                balanceSummary = repository.getCardBalanceSummary(it)
            }
        }
    }

    val card = cardState
    if (card == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Card Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IndigoPrimary)
            }
        }
        return
    }

    val cardColor = remember(card.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.colorHex))
        } catch (e: Exception) {
            CardBlue
        }
    }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = if (isPrivacyMode) "••••••••" else card.name,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                isPrivacyMode = isPrivacyMode,
                onTogglePrivacy = {
                    isPrivacyMode = securityManager.togglePrivacyMode()
                },
                onLockApp = onLockApp,
                onOpenSettings = null
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Realistic Visual Card Presentation
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.issuingBank ?: card.category.replace("_", " "),
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (card.isTrackingEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldDark.copy(alpha = 0.9f)
                                    ) {
                                        Text(
                                            text = "TRACKED",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(
                                    imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (card.isFavorite) AmberWarning else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Chip graphic
                        Box(
                            modifier = Modifier
                                .size(36.dp, 28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE2B93B))
                        )

                        // Masked Card Number
                        Text(
                            text = CurrencyFormatter.maskCardNumber(card.cardNumber, isPrivacyMode),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "CARDHOLDER",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isPrivacyMode) "••••••••" else (card.cardholderName ?: "PERSONAL VAULT"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "EXPIRES",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = card.expiryDate ?: "N/A",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Interactive Masked Number with Eye & Copy
            item {
                MaskedCardNumber(
                    cardNumber = card.cardNumber,
                    isPrivacyMode = isPrivacyMode,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Quick Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onAddExpense(card.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Expense")
                    }

                    Button(
                        onClick = { onAddNote(card.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberWarning)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Note", color = TextPrimaryDark)
                    }

                    Button(
                        onClick = { onEditCard(card.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondaryDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", color = TextPrimaryDark)
                    }
                }
            }

            // 4. Optional Balance Tracking Section
            if (card.isTrackingEnabled) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Card Payment Balance Ledger",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Active Tracking",
                                        color = EmeraldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Current Available Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = CurrencyFormatter.format(balanceSummary.currentBalance, card.currency, isPrivacyMode),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (balanceSummary.currentBalance >= 0) EmeraldAccent else CrimsonError
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Formula: Opening (${CurrencyFormatter.format(balanceSummary.openingBalance, card.currency, isPrivacyMode)}) + Credits (${CurrencyFormatter.format(balanceSummary.totalCredits, card.currency, isPrivacyMode)}) + Refunds (${CurrencyFormatter.format(balanceSummary.totalRefunds, card.currency, isPrivacyMode)}) − Expenses (${CurrencyFormatter.format(balanceSummary.totalExpenses, card.currency, isPrivacyMode)})",
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
                                    Text("Opening Balance", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Text(CurrencyFormatter.format(balanceSummary.openingBalance, card.currency, isPrivacyMode), fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }
                                Column {
                                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Text(CurrencyFormatter.format(balanceSummary.totalExpenses, card.currency, isPrivacyMode), fontWeight = FontWeight.Bold, color = CrimsonError)
                                }
                                Column {
                                    Text("Credits/Refunds", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                    Text(CurrencyFormatter.format(balanceSummary.totalCredits + balanceSummary.totalRefunds, card.currency, isPrivacyMode), fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Front & Back Photos Viewer
            if (!card.frontImagePath.isNullOrBlank() || !card.backImagePath.isNullOrBlank()) {
                item {
                    Text(
                        text = "Card Photographs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!card.frontImagePath.isNullOrBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Front Side", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                AsyncImage(
                                    model = File(card.frontImagePath!!),
                                    contentDescription = "Front Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianSurface)
                                )
                            }
                        }
                        if (!card.backImagePath.isNullOrBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Back Side", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                AsyncImage(
                                    model = File(card.backImagePath!!),
                                    contentDescription = "Back Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianSurface)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Card Information Attributes Table
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Card Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow("Category", card.category.replace("_", " "))
                        DetailRow("Card Type", card.cardType)
                        if (!card.issuingBank.isNullOrBlank()) DetailRow("Issuing Entity", card.issuingBank!!)
                        if (!card.cardholderName.isNullOrBlank()) DetailRow("Cardholder", if (isPrivacyMode) "••••••••" else card.cardholderName!!)
                        if (!card.issueDate.isNullOrBlank()) DetailRow("Issue Date", card.issueDate!!)
                        if (!card.expiryDate.isNullOrBlank()) DetailRow("Expiry Date", card.expiryDate!!)
                        if (!card.contactNumber.isNullOrBlank()) DetailRow("Contact Phone", card.contactNumber!!)
                        if (!card.website.isNullOrBlank()) DetailRow("Website", card.website!!)
                        if (!card.address.isNullOrBlank()) DetailRow("Address", card.address!!)
                        if (!card.notes.isNullOrBlank()) DetailRow("Notes", card.notes!!)

                        // Dynamic Custom Fields
                        card.customFields.forEach { (key, value) ->
                            DetailRow(key, value)
                        }

                        DetailRow("Created Date", DateUtil.formatDate(card.createdAt))
                        DetailRow("Last Updated", DateUtil.formatDate(card.updatedAt))
                    }
                }
            }

            // 7. Linked Transactions Section
            if (card.isTrackingEnabled) {
                item {
                    Text(
                        text = "Card Transactions (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        Text("No transactions recorded on this card yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    }
                } else {
                    items(transactions) { tx ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.description, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    Text("${tx.category} • ${DateUtil.formatDateTime(tx.date)}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                                }
                                Text(
                                    text = (if (tx.type == "EXPENSE") "- " else "+ ") + CurrencyFormatter.format(tx.amount, tx.currency, isPrivacyMode),
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "EXPENSE") CrimsonError else EmeraldAccent
                                )
                            }
                        }
                    }
                }
            }

            // 8. Delete / Archive Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.saveCard(card.copy(isArchived = !card.isArchived))
                                Toast.makeText(context, if (card.isArchived) "Card restored" else "Card archived", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (card.isArchived) "Unarchive" else "Archive Card", color = TextSecondaryDark)
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonError.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete Card", color = CrimsonError, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Card", color = TextPrimaryDark) },
            text = {
                Text(
                    "Are you sure you want to permanently delete '${card.name}'? This cannot be undone.",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteCard(card)
                            Toast.makeText(context, "Card deleted.", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryDark
        )
    }
}
