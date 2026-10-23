package com.smartvault.app.ui.screens.scanner

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartvault.app.data.local.entity.CardEntity
import com.smartvault.app.data.local.entity.VisitingContactEntity
import com.smartvault.app.data.repository.VaultRepository
import com.smartvault.app.scanner.ParsedCardData
import com.smartvault.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReviewScreen(
    parsedData: ParsedCardData,
    frontImagePath: String?,
    backImagePath: String?,
    repository: VaultRepository,
    onNavigateBack: () -> Unit,
    onSavedSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(parsedData.name ?: "Scanned Card") }
    var category by remember { mutableStateOf(parsedData.suggestedCategory) }
    var cardholderName by remember { mutableStateOf(parsedData.cardholderName ?: "") }
    var cardNumber by remember { mutableStateOf(parsedData.cardNumber ?: "") }
    var issuingBank by remember { mutableStateOf(parsedData.issuingBank ?: "") }
    var expiryDate by remember { mutableStateOf(parsedData.expiryDate ?: "") }
    var phone by remember { mutableStateOf(parsedData.phone ?: "") }
    var email by remember { mutableStateOf(parsedData.email ?: "") }
    var website by remember { mutableStateOf(parsedData.website ?: "") }
    var designation by remember { mutableStateOf(parsedData.designation ?: "") }
    var company by remember { mutableStateOf(parsedData.company ?: "") }
    var saveAsVisitingContact by remember { mutableStateOf(parsedData.suggestedCategory == "VISITING") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Extracted OCR Data", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
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
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldDark.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OCR scan successful! Please review and verify the recognized details below before saving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryDark
                        )
                    }
                }
            }

            // Preview Captured Photos
            if (frontImagePath != null || backImagePath != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (frontImagePath != null) {
                            AsyncImage(
                                model = File(frontImagePath),
                                contentDescription = "Front",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianSurface)
                            )
                        }
                        if (backImagePath != null) {
                            AsyncImage(
                                model = File(backImagePath),
                                contentDescription = "Back",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianSurface)
                            )
                        }
                    }
                }
            }

            // Category Picker
            item {
                Text("Select Category", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                val categories = listOf(
                    "BANK_PAYMENT" to "Bank Card",
                    "VISITING" to "Visiting Card",
                    "IDENTITY_MEMBERSHIP" to "Identity / ID",
                    "PERSONAL" to "Personal"
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

            // Editable Fields
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card / Record Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (category == "BANK_PAYMENT" || cardNumber.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Extracted Card Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    label = { Text("Recognized Name / Contact") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (category == "BANK_PAYMENT") {
                item {
                    OutlinedTextField(
                        value = issuingBank,
                        onValueChange = { issuingBank = it },
                        label = { Text("Issuing Bank / Network") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry Date (MM/YY)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (category == "VISITING" || phone.isNotBlank() || email.isNotBlank()) {
                item {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company / Organization") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("Designation / Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryDark, unfocusedTextColor = TextPrimaryDark),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = saveAsVisitingContact,
                            onCheckedChange = { saveAsVisitingContact = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Also save into Visiting Card Contacts Directory",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryDark
                        )
                    }
                }
            }

            // Save Confirmation Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val card = CardEntity(
                                name = name.trim().ifBlank { "Scanned Card" },
                                category = category,
                                cardholderName = cardholderName.trim().ifBlank { null },
                                cardNumber = cardNumber.trim().ifBlank { null },
                                issuingBank = issuingBank.trim().ifBlank { null },
                                expiryDate = expiryDate.trim().ifBlank { null },
                                contactNumber = phone.trim().ifBlank { null },
                                website = website.trim().ifBlank { null },
                                frontImagePath = frontImagePath,
                                backImagePath = backImagePath,
                                colorHex = when (category) {
                                    "BANK_PAYMENT" -> "#065F46"
                                    "VISITING" -> "#78350F"
                                    "IDENTITY_MEMBERSHIP" -> "#581C87"
                                    else -> "#1E3A8A"
                                },
                                cardType = when (category) {
                                    "BANK_PAYMENT" -> "DEBIT"
                                    "VISITING" -> "VISITING"
                                    "IDENTITY_MEMBERSHIP" -> "NATIONAL_ID"
                                    else -> "CUSTOM"
                                }
                            )
                            val insertedCardId = repository.saveCard(card)

                            if (saveAsVisitingContact) {
                                val contact = VisitingContactEntity(
                                    name = cardholderName.ifBlank { name },
                                    company = company.ifBlank { null },
                                    designation = designation.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    email = email.ifBlank { null },
                                    website = website.ifBlank { null },
                                    cardId = insertedCardId,
                                    frontImagePath = frontImagePath
                                )
                                repository.saveVisitingContact(contact)
                            }

                            Toast.makeText(context, "Card verified and saved to vault!", Toast.LENGTH_SHORT).show()
                            onSavedSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Confirm & Save to Vault", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
