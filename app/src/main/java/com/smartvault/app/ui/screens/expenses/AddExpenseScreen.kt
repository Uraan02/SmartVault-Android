package com.smartvault.app.ui.screens.expenses

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.local.entity.TransactionEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    initialCardId: Long? = null,
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allCards by repository.getAllCards().collectAsState(initial = emptyList())
    val familyMembers by repository.getAllFamilyMembers().collectAsState(initial = emptyList())

    var type by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, REFUND
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var selectedCardId by remember { mutableStateOf(initialCardId) }
    var selectedFamilyMemberId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }

    val categories = listOf(
        "Food", "Transport", "Fuel", "Shopping", "Bills", "Business", "Medical", "Education", "Custom"
    )

    val selectedCard = allCards.find { it.id == selectedCardId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Transaction", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val types = listOf(
                        "EXPENSE" to ("Expense" to CrimsonError),
                        "INCOME" to ("Income" to EmeraldAccent),
                        "REFUND" to ("Refund" to AmberWarning)
                    )
                    types.forEach { (tKey, data) ->
                        val (label, color) = data
                        val isSelected = type == tKey
                        Button(
                            onClick = { type = tKey },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) color else ObsidianSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Amount Input
            item {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (${securityManager.getDefaultCurrency()}) *") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = if (type == "EXPENSE") CrimsonError else EmeraldAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Merchant *") },
                    placeholder = { Text("e.g. Grocery Store, Shell Fuel, Salary") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Chips
            item {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Linked Card Picker
            item {
                Text("Payment Method / Linked Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCardId == null,
                            onClick = { selectedCardId = null },
                            label = { Text("Cash / General Ledger") }
                        )
                    }
                    items(allCards) { card ->
                        FilterChip(
                            selected = selectedCardId == card.id,
                            onClick = { selectedCardId = card.id },
                            label = {
                                Text(card.name + if (card.isTrackingEnabled) " (Tracked)" else "")
                            }
                        )
                    }
                }

                // Balance Deduct Explanatory Note
                if (selectedCard != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (selectedCard.isTrackingEnabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldAccent.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Automatic deduction: This transaction will be automatically applied to '${selectedCard.name}' tracked balance.",
                                color = EmeraldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ℹ️ Note: Tracking is OFF on '${selectedCard.name}'. Recorded in general expense manager without altering card balance.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Linked Family Member
            if (familyMembers.isNotEmpty()) {
                item {
                    Text("Associated Family Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFamilyMemberId == null,
                                onClick = { selectedFamilyMemberId = null },
                                label = { Text("None") }
                            )
                        }
                        items(familyMembers) { member ->
                            FilterChip(
                                selected = selectedFamilyMemberId == member.id,
                                onClick = { selectedFamilyMemberId = member.id },
                                label = { Text(member.name) }
                            )
                        }
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Save Transaction Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (description.isBlank()) {
                            Toast.makeText(context, "Please enter a description.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            val tx = TransactionEntity(
                                type = type,
                                amount = amount,
                                currency = securityManager.getDefaultCurrency(),
                                category = category,
                                description = description.trim(),
                                cardId = selectedCardId,
                                familyMemberId = selectedFamilyMemberId,
                                notes = notes.trim().ifBlank { null }
                            )
                            repository.recordTransaction(tx)
                            Toast.makeText(context, "Transaction saved successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "EXPENSE") CrimsonError else EmeraldAccent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Confirm & Record Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
