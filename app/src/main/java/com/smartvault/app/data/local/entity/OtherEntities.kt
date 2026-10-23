package com.smartvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "credit_card",
    val colorHex: String = "#6366F1",
    val isSystem: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: String, // Self, Spouse, Child, Parent, Sibling, Other
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val avatarColorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long? = null, // Linked card if balance tracking is enabled
    val type: String, // EXPENSE, INCOME, REFUND, ADJUSTMENT
    val amount: Double,
    val currency: String = "PKR",
    val category: String, // Food, Transport, Fuel, Shopping, Bills, Business, Medical, Education, Custom
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val receiptImagePath: String? = null,
    val familyMemberId: Long? = null,
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null, // MONTHLY, WEEKLY, YEARLY
    val createdAt: Long = System.currentTimeMillis()
)

data class ChecklistItem(
    val id: String,
    val text: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val cardId: Long? = null,
    val familyMemberId: Long? = null,
    val contactId: Long? = null,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null,
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "visiting_contacts")
data class VisitingContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val designation: String? = null,
    val company: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val cardId: Long? = null,
    val frontImagePath: String? = null,
    val tags: List<String> = emptyList(), // Client, Supplier, Friend, Lead, Partner, Custom
    val meetingNotes: String? = null,
    val lastContactDate: Long? = null,
    val nextFollowUpDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String, // CARD_CREATED, CARD_UPDATED, CARD_DELETED, TRANSACTION_ADDED, TRANSACTION_DELETED, BALANCE_ADJUSTED, NOTE_CREATED
    val entityType: String, // CARD, TRANSACTION, NOTE, CONTACT
    val entityId: Long,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
