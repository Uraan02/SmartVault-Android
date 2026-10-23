package com.smartvault.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.smartvault.app.ui.theme.*
import com.smartvault.app.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseReportsScreen(
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit
) {
    val transactions by repository.getAllTransactions().collectAsState(initial = emptyList())
    val isPrivacyMode = securityManager.isPrivacyModeEnabled()

    val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    val categoryBreakdown = remember(expenses, totalExpense) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categoryColors = listOf(
        IndigoPrimary, EmeraldAccent, AmberWarning, RosePink, CyanInfo, VioletPurple, Color(0xFFF97316), Color(0xFF14B8A6)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Reports & Analytics", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
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
                // Overall Total Card
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
                        Text("Total Recorded Outflow", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.format(totalExpense, securityManager.getDefaultCurrency(), isPrivacyMode),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimsonError
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Across ${expenses.size} expenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiaryDark
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            if (categoryBreakdown.isEmpty()) {
                item {
                    Text("No expenses recorded to analyze.", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                }
            } else {
                items(categoryBreakdown.withIndex().toList()) { (index, item) ->
                    val (categoryName, amount) = item
                    val percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                    val color = categoryColors[index % categoryColors.size]

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(categoryName, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }
                                Text(
                                    text = CurrencyFormatter.format(amount, securityManager.getDefaultCurrency(), isPrivacyMode),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = color,
                                trackColor = ObsidianSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(percentage * 100).toInt()}% of total expenses",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
