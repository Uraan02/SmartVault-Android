package com.smartvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // PERSONAL, BANK_PAYMENT, FAMILY, VISITING, IDENTITY_MEMBERSHIP, CUSTOM
    val customCategoryName: String? = null,
    val cardholderName: String? = null,
    val cardNumber: String? = null, // Masked by default
    val issuingBank: String? = null,
    val issueDate: String? = null,
    val expiryDate: String? = null, // e.g. "08/28" or "2028-08-31"
    val contactNumber: String? = null,
    val website: String? = null,
    val address: String? = null,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val colorHex: String = "#1E3A8A",
    val cardType: String = "DEBIT", // DEBIT, CREDIT, NATIONAL_ID, DRIVING_LICENSE, PASSPORT, VISITING, MEMBERSHIP, CUSTOM
    val familyMemberId: Long? = null,
    val tags: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val notes: String? = null,
    val isTrackingEnabled: Boolean = false, // OFF by default
    val openingBalance: Double = 0.0,
    val currency: String = "PKR",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
