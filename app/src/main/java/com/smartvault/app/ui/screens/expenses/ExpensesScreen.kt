package com.smartvault.app.ui.screens.expenses

import android.content.Intent
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
import com.smartvault.app.data.local.entity.TransactionEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.components.EmptyVaultState
import com.smartvault.app.ui.components.SmartVaultBottomNavigation
import com.smartvault.app.ui.components.SmartVaultTopBar
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.CsvExporter
import com.smartvault.app.util.CurrencyFormatter
import com.smartvault.app.util.DateUtil
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val transactions by repository.getAllTransactions().collectAsState(initial = emptyList())
    val allCards by repository.getAllCards().collectAsState(initial = emptyList())

    var isPrivacyMode by remember { mutableStateOf(securityManager.isPrivacyModeEnabled()) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME, REFUND
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("Food", "Transport", "Fuel", "Shopping", "Bills", "Business", "Medical", "Education", "Custom")

    val filteredTransactions = remember(transactions, selectedTypeFilter, selectedCategoryFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesType = when (selectedTypeFilter) {
                "EXPENSE" -> tx.type == "EXPENSE"
                "INCOME" -> tx.type == "INCOME" || tx.type == "CREDIT"
                "REFUND" -> tx.type == "REFUND"
                else -> true
            }
            val matchesCategory = if (selectedCategoryFilter == null) true else tx.category == selectedCategoryFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.description.contains(searchQuery, ignoreCase = true) ||
                        tx.category.contains(searchQuery, ignoreCase = true)
            }
            matchesType && matchesCategory && matchesSearch
        }
    }

    // Totals
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" || it.type == "CREDIT" }.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            SmartVaultTopBar(
                title = "Expense Manager",
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
                currentRoute = "expenses",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate("add_expense") },
                containerColor = EmeraldAccent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Summary Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ledger Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        TextButton(onClick = { onNavigate("expense_reports") }) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = IndigoLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Visual Reports", color = IndigoLight, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(
                                text = CurrencyFormatter.format(totalExpense, securityManager.getDefaultCurrency(), isPrivacyMode),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimsonError
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Income / Credits", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(
                                text = CurrencyFormatter.format(totalIncome, securityManager.getDefaultCurrency(), isPrivacyMode),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }

            // Search Bar & CSV Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = {
                        if (transactions.isEmpty()) {
                            Toast.makeText(context, "No transactions to export.", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val csv = CsvExporter.exportTransactions(transactions)
                        val cacheFile = File(context.cacheDir, "smartvault_transactions.csv")
                        cacheFile.writeText(csv)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "SmartVault_Transactions.csv")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Transactions (.csv)"))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        tint = EmeraldAccent
                    )
                }
            }

            // Type Filters (All, Expenses, Income, Refunds)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == "ALL",
                    onClick = { selectedTypeFilter = "ALL" },
                    label = { Text("All (${transactions.size})") }
                )
                FilterChip(
                    selected = selectedTypeFilter == "EXPENSE",
                    onClick = { selectedTypeFilter = "EXPENSE" },
                    label = { Text("Expenses") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonError.copy(alpha = 0.2f),
                        selectedLabelColor = CrimsonError
                    )
                )
                FilterChip(
                    selected = selectedTypeFilter == "INCOME",
                    onClick = { selectedTypeFilter = "INCOME" },
                    label = { Text("Income") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldAccent.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldAccent
                    )
                )
            }

            // Transactions List
            if (filteredTransactions.isEmpty()) {
                EmptyVaultState(
                    title = "No transactions found",
                    subtitle = "Log an expense or deposit to keep track of your cashflow and card balances.",
                    actionText = "Record Expense",
                    onAction = { onNavigate("add_expense") }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTransactions) { tx ->
                        val linkedCard = allCards.find { it.id == tx.cardId }
                        TransactionRow(
                            transaction = tx,
                            linkedCardName = linkedCard?.name,
                            isPrivacyMode = isPrivacyMode,
                            onDelete = {
                                coroutineScope.launch {
                                    repository.deleteTransaction(tx)
                                    Toast.makeText(context, "Transaction deleted.", Toast.LENGTH_SHORT).show()
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
fun TransactionRow(
    transaction: TransactionEntity,
    linkedCardName: String?,
    isPrivacyMode: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (transaction.category) {
                "Food" -> Icons.Default.Restaurant
                "Transport", "Fuel" -> Icons.Default.DirectionsCar
                "Shopping" -> Icons.Default.ShoppingBag
                "Bills" -> Icons.Default.Receipt
                "Medical" -> Icons.Default.LocalHospital
                "Education" -> Icons.Default.School
                else -> Icons.Default.Payment
            }
            val isExpense = transaction.type == "EXPENSE"

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isExpense) CrimsonError.copy(alpha = 0.15f) else EmeraldAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isExpense) CrimsonError else EmeraldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${transaction.category} • ${DateUtil.formatDate(transaction.date)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    if (linkedCardName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndigoPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = linkedCardName,
                                color = IndigoLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isExpense) "- " else "+ ") + CurrencyFormatter.format(transaction.amount, transaction.currency, isPrivacyMode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) CrimsonError else EmeraldAccent
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextTertiaryDark, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
