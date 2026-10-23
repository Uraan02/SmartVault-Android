package com.smartvault.app.ui.screens.cards

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.security.SecurityManager
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: Long? = null,
    repository: VaultRepository,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit,
    onLaunchScanner: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val familyMembers by repository.getAllFamilyMembers().collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PERSONAL") }
    var cardholderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var issuingBank by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#1E3A8A") }
    var cardType by remember { mutableStateOf("DEBIT") }
    var selectedFamilyMemberId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }

    // Balance Tracking (OFF by default)
    var isTrackingEnabled by remember { mutableStateOf(false) }
    var openingBalanceStr by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf(securityManager.getDefaultCurrency()) }

    // Dynamic Custom Fields
    var customFields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newFieldKey by remember { mutableStateOf("") }
    var newFieldValue by remember { mutableStateOf("") }
    var showCustomFieldDialog by remember { mutableStateOf(false) }

    // Load existing card if editing
    LaunchedEffect(cardId) {
        if (cardId != null && cardId != 0L) {
            repository.getCardById(cardId).collect { existing ->
                existing?.let {
                    name = it.name
                    category = it.category
                    cardholderName = it.cardholderName ?: ""
                    cardNumber = it.cardNumber ?: ""
                    issuingBank = it.issuingBank ?: ""
                    issueDate = it.issueDate ?: ""
                    expiryDate = it.expiryDate ?: ""
                    contactNumber = it.contactNumber ?: ""
                    website = it.website ?: ""
                    address = it.address ?: ""
                    colorHex = it.colorHex
                    cardType = it.cardType
                    selectedFamilyMemberId = it.familyMemberId
                    notes = it.notes ?: ""
                    isTrackingEnabled = it.isTrackingEnabled
                    openingBalanceStr = it.openingBalance.toString()
                    currency = it.currency
                    customFields = it.customFields
                }
            }
        }
    }

    val availableColors = listOf(
        "#1E3A8A", "#065F46", "#581C87", "#78350F", "#1E293B", "#881337", "#0F766E", "#312E81"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (cardId != null && cardId != 0L) "Edit Card" else "Add New Card",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                    }
                },
                actions = {
                    IconButton(onClick = onLaunchScanner) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan with Camera", tint = IndigoPrimary)
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
            // Scanner Prompt Card
            item {
                Surface(
                    onClick = onLaunchScanner,
                    shape = RoundedCornerShape(14.dp),
                    color = IndigoPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = IndigoLight)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fast OCR Card Scanner", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("Auto-fill card details using camera recognition", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = IndigoLight)
                    }
                }
            }

            // Category Picker
            item {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                val categories = listOf(
                    "PERSONAL" to "Personal",
                    "BANK_PAYMENT" to "Bank Card",
                    "FAMILY" to "Family",
                    "VISITING" to "Visiting Card",
                    "IDENTITY_MEMBERSHIP" to "Identity / ID",
                    "CUSTOM" to "Custom"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { (catKey, catLabel) ->
                        FilterChip(
                            selected = category == catKey,
                            onClick = { category = catKey },
                            label = { Text(catLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Card Color Picker
            item {
                Text("Card Visual Accent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableColors.forEach { hex ->
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // General Fields
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Nickname / Title *") },
                    placeholder = { Text("e.g. HBL World Elite, CNIC, Dr. Ali Card") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Cardholder / Contact Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Card Number / Reference / ID") },
                    placeholder = { Text("e.g. 5241 •••• •••• 1234") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "🔒 Security guarantee: CVVs and PINs are NEVER stored in SmartVault.",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldAccent,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = issuingBank,
                    onValueChange = { issuingBank = it },
                    label = { Text("Issuing Bank / Organization / Company") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = issueDate,
                        onValueChange = { issueDate = it },
                        label = { Text("Issue Date") },
                        placeholder = { Text("e.g. 08/23") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date") },
                        placeholder = { Text("e.g. 08/28") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Family Member Assignment
            if (familyMembers.isNotEmpty()) {
                item {
                    Text("Linked Family Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedFamilyMemberId == null,
                                onClick = { selectedFamilyMemberId = null },
                                label = { Text("None / General") }
                            )
                        }
                        items(familyMembers) { member ->
                            FilterChip(
                                selected = selectedFamilyMemberId == member.id,
                                onClick = { selectedFamilyMemberId = member.id },
                                label = { Text("${member.name} (${member.relationship})") }
                            )
                        }
                    }
                }
            }

            // Contact Info
            item {
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Phone / Helpline") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Balance & Payment Tracking Section (OFF by default)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Balance & Payment Tracking",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Keep a manual ledger of deposits, refunds, and card expenses.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                            Switch(
                                checked = isTrackingEnabled,
                                onCheckedChange = { isTrackingEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldAccent,
                                    checkedTrackColor = EmeraldAccent.copy(alpha = 0.3f)
                                )
                            )
                        }

                        if (isTrackingEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = ObsidianBorder, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = openingBalanceStr,
                                    onValueChange = { openingBalanceStr = it },
                                    label = { Text("Opening Balance") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                                    modifier = Modifier.weight(1.5f)
                                )
                                OutlinedTextField(
                                    value = currency,
                                    onValueChange = { currency = it },
                                    label = { Text("Currency") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Notice: This is manual recordkeeping only. It will never connect to or deduct money from a real bank account.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning
                            )
                        }
                    }
                }
            }

            // Custom Dynamic Fields
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom Fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    TextButton(onClick = { showCustomFieldDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Field", color = IndigoLight)
                    }
                }
                customFields.forEach { (key, value) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianSurface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$key: $value", color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                            IconButton(
                                onClick = {
                                    customFields = customFields.filterKeys { it != key }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Private Notes") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter a card name.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val opBal = openingBalanceStr.toDoubleOrNull() ?: 0.0

                        coroutineScope.launch {
                            val existing = repository.checkDuplicateCard(cardNumber, name, cardId ?: 0L)
                            if (existing != null && cardId == null) {
                                Toast.makeText(context, "Notice: A card with similar details already exists: '${existing.name}'", Toast.LENGTH_LONG).show()
                            }

                            val cardToSave = CardEntity(
                                id = cardId ?: 0L,
                                name = name.trim(),
                                category = category,
                                cardholderName = cardholderName.trim().ifBlank { null },
                                cardNumber = cardNumber.trim().ifBlank { null },
                                issuingBank = issuingBank.trim().ifBlank { null },
                                issueDate = issueDate.trim().ifBlank { null },
                                expiryDate = expiryDate.trim().ifBlank { null },
                                contactNumber = contactNumber.trim().ifBlank { null },
                                website = website.trim().ifBlank { null },
                                address = address.trim().ifBlank { null },
                                colorHex = colorHex,
                                cardType = cardType,
                                familyMemberId = selectedFamilyMemberId,
                                notes = notes.trim().ifBlank { null },
                                isTrackingEnabled = isTrackingEnabled,
                                openingBalance = opBal,
                                currency = currency,
                                customFields = customFields,
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.saveCard(cardToSave)
                            Toast.makeText(context, "Card saved to vault!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (cardId != null && cardId != 0L) "Update Card" else "Save Card to Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showCustomFieldDialog) {
        AlertDialog(
            onDismissRequest = { showCustomFieldDialog = false },
            title = { Text("Add Custom Field", color = TextPrimaryDark) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFieldKey,
                        onValueChange = { newFieldKey = it },
                        label = { Text("Field Name (e.g. Branch Code, PIN Reference)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFieldValue,
                        onValueChange = { newFieldValue = it },
                        label = { Text("Field Value") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFieldKey.isNotBlank()) {
                            customFields = customFields + (newFieldKey.trim() to newFieldValue.trim())
                            newFieldKey = ""
                            newFieldValue = ""
                            showCustomFieldDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFieldDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}
