package com.smartvault.app.data.repository

import android.content.Context
import com.smartvault.app.data.local.SmartVaultDatabase
import com.smartvault.app.data.local.entity.*
import com.smartvault.app.security.BackupEncryption
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class CardBalanceSummary(
    val openingBalance: Double,
    val currentBalance: Double,
    val totalCredits: Double,
    val totalExpenses: Double,
    val totalRefunds: Double,
    val transactionCount: Int
)

class VaultRepository(private val context: Context) {
    private val db = SmartVaultDatabase.getDatabase(context)
    private val cardDao = db.cardDao()
    private val categoryDao = db.categoryDao()
    private val familyMemberDao = db.familyMemberDao()
    private val transactionDao = db.transactionDao()
    private val noteDao = db.noteDao()
    private val visitingContactDao = db.visitingContactDao()
    private val activityLogDao = db.activityLogDao()

    // Cards
    fun getAllCards(): Flow<List<CardEntity>> = cardDao.getAllActiveCards()
    fun getCardsByCategory(category: String): Flow<List<CardEntity>> = cardDao.getCardsByCategory(category)
    fun getFavoriteCards(): Flow<List<CardEntity>> = cardDao.getFavoriteCards()
    fun getArchivedCards(): Flow<List<CardEntity>> = cardDao.getArchivedCards()
    fun getTrackedCards(): Flow<List<CardEntity>> = cardDao.getTrackedCards()
    fun getCardsByFamilyMember(memberId: Long): Flow<List<CardEntity>> = cardDao.getCardsByFamilyMember(memberId)
    fun getCardById(id: Long): Flow<CardEntity?> = cardDao.getCardByIdFlow(id)
    fun searchCards(query: String): Flow<List<CardEntity>> = cardDao.searchCards(query)

    suspend fun saveCard(card: CardEntity): Long {
        val id = if (card.id == 0L) {
            val insertedId = cardDao.insertCard(card)
            activityLogDao.insertLog(
                ActivityLogEntity(
                    action = "CARD_CREATED",
                    entityType = "CARD",
                    entityId = insertedId,
                    description = "Added card: ${card.name} (${card.category})"
                )
            )
            insertedId
        } else {
            cardDao.updateCard(card)
            activityLogDao.insertLog(
                ActivityLogEntity(
                    action = "CARD_UPDATED",
                    entityType = "CARD",
                    entityId = card.id,
                    description = "Updated card: ${card.name}"
                )
            )
            card.id
        }
        return id
    }

    suspend fun deleteCard(card: CardEntity) {
        cardDao.deleteCard(card)
        activityLogDao.insertLog(
            ActivityLogEntity(
                action = "CARD_DELETED",
                entityType = "CARD",
                entityId = card.id,
                description = "Deleted card: ${card.name}"
            )
        )
    }

    suspend fun checkDuplicateCard(cardNumber: String?, name: String, excludeId: Long = 0): CardEntity? {
        if (!cardNumber.isNullOrBlank()) {
            val byNumber = cardDao.findDuplicateCardByNumber(cardNumber, excludeId)
            if (byNumber != null) return byNumber
        }
        return cardDao.findDuplicateCardByName(name, excludeId)
    }

    // Card Balance Tracking
    suspend fun getCardBalanceSummary(card: CardEntity): CardBalanceSummary {
        if (!card.isTrackingEnabled) {
            return CardBalanceSummary(
                openingBalance = 0.0,
                currentBalance = 0.0,
                totalCredits = 0.0,
                totalExpenses = 0.0,
                totalRefunds = 0.0,
                transactionCount = 0
            )
        }
        val transactions = transactionDao.getTransactionsByCardList(card.id)
        var credits = 0.0
        var expenses = 0.0
        var refunds = 0.0

        for (tx in transactions) {
            when (tx.type) {
                "INCOME", "CREDIT" -> credits += tx.amount
                "EXPENSE" -> expenses += tx.amount
                "REFUND" -> refunds += tx.amount
                "ADJUSTMENT" -> {
                    if (tx.amount >= 0) credits += tx.amount else expenses += Math.abs(tx.amount)
                }
            }
        }

        // Current balance = Opening balance + Credits + Refunds - Expenses
        val current = card.openingBalance + credits + refunds - expenses

        return CardBalanceSummary(
            openingBalance = card.openingBalance,
            currentBalance = current,
            totalCredits = credits,
            totalExpenses = expenses,
            totalRefunds = refunds,
            transactionCount = transactions.size
        )
    }

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    suspend fun addCategory(category: CategoryEntity) = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Family Members
    fun getAllFamilyMembers(): Flow<List<FamilyMemberEntity>> = familyMemberDao.getAllFamilyMembers()
    fun getFamilyMemberById(id: Long): Flow<FamilyMemberEntity?> = familyMemberDao.getFamilyMemberByIdFlow(id)
    suspend fun saveFamilyMember(member: FamilyMemberEntity): Long = familyMemberDao.insertFamilyMember(member)
    suspend fun deleteFamilyMember(member: FamilyMemberEntity) = familyMemberDao.deleteFamilyMember(member)

    // Transactions / Expenses
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    fun getTransactionsByCard(cardId: Long): Flow<List<TransactionEntity>> = transactionDao.getTransactionsByCard(cardId)
    fun getTransactionsByFamilyMember(memberId: Long): Flow<List<TransactionEntity>> = transactionDao.getTransactionsByFamilyMember(memberId)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>> = transactionDao.searchTransactions(query)

    suspend fun recordTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        activityLogDao.insertLog(
            ActivityLogEntity(
                action = "TRANSACTION_ADDED",
                entityType = "TRANSACTION",
                entityId = id,
                description = "${transaction.type}: ${transaction.currency} ${transaction.amount} - ${transaction.description}"
            )
        )
        return id
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        activityLogDao.insertLog(
            ActivityLogEntity(
                action = "TRANSACTION_DELETED",
                entityType = "TRANSACTION",
                entityId = transaction.id,
                description = "Deleted transaction: ${transaction.description}"
            )
        )
    }

    // Notes & Reminders
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    fun getNotesByCard(cardId: Long): Flow<List<NoteEntity>> = noteDao.getNotesByCard(cardId)
    fun getNotesByFamilyMember(memberId: Long): Flow<List<NoteEntity>> = noteDao.getNotesByFamilyMember(memberId)
    fun getNotesByContact(contactId: Long): Flow<List<NoteEntity>> = noteDao.getNotesByContact(contactId)
    fun getUpcomingReminders(): Flow<List<NoteEntity>> = noteDao.getUpcomingReminders()
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun saveNote(note: NoteEntity): Long = noteDao.insertNote(note)
    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    // Visiting Contacts
    fun getAllVisitingContacts(): Flow<List<VisitingContactEntity>> = visitingContactDao.getAllContacts()
    fun getVisitingContactById(id: Long): Flow<VisitingContactEntity?> = visitingContactDao.getContactByIdFlow(id)
    fun searchContacts(query: String): Flow<List<VisitingContactEntity>> = visitingContactDao.searchContacts(query)
    suspend fun saveVisitingContact(contact: VisitingContactEntity): Long = visitingContactDao.insertContact(contact)
    suspend fun deleteVisitingContact(contact: VisitingContactEntity) = visitingContactDao.deleteContact(contact)

    // Activity Logs
    fun getRecentActivities(): Flow<List<ActivityLogEntity>> = activityLogDao.getRecentActivities()

    // Export & Encrypted Backup
    suspend fun createEncryptedBackup(password: CharArray): ByteArray {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Cards
        val cards = cardDao.getAllCardsForExport()
        val cardsArray = JSONArray()
        for (c in cards) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("category", c.category)
            obj.put("customCategoryName", c.customCategoryName)
            obj.put("cardholderName", c.cardholderName)
            obj.put("cardNumber", c.cardNumber)
            obj.put("issuingBank", c.issuingBank)
            obj.put("issueDate", c.issueDate)
            obj.put("expiryDate", c.expiryDate)
            obj.put("contactNumber", c.contactNumber)
            obj.put("website", c.website)
            obj.put("address", c.address)
            obj.put("colorHex", c.colorHex)
            obj.put("cardType", c.cardType)
            obj.put("isFavorite", c.isFavorite)
            obj.put("isArchived", c.isArchived)
            obj.put("notes", c.notes)
            obj.put("isTrackingEnabled", c.isTrackingEnabled)
            obj.put("openingBalance", c.openingBalance)
            obj.put("currency", c.currency)
            cardsArray.put(obj)
        }
        root.put("cards", cardsArray)

        // Family
        val family = familyMemberDao.getAllForExport()
        val familyArray = JSONArray()
        for (f in family) {
            val obj = JSONObject()
            obj.put("name", f.name)
            obj.put("relationship", f.relationship)
            obj.put("phone", f.phone)
            obj.put("email", f.email)
            obj.put("notes", f.notes)
            obj.put("avatarColorHex", f.avatarColorHex)
            familyArray.put(obj)
        }
        root.put("family", familyArray)

        // Transactions
        val txs = transactionDao.getAllForExport()
        val txArray = JSONArray()
        for (t in txs) {
            val obj = JSONObject()
            obj.put("type", t.type)
            obj.put("amount", t.amount)
            obj.put("currency", t.currency)
            obj.put("category", t.category)
            obj.put("description", t.description)
            obj.put("date", t.date)
            obj.put("notes", t.notes)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Notes
        val notes = noteDao.getAllForExport()
        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("isPinned", n.isPinned)
            obj.put("isArchived", n.isArchived)
            obj.put("hasReminder", n.hasReminder)
            obj.put("reminderTime", n.reminderTime)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // Contacts
        val contacts = visitingContactDao.getAllForExport()
        val contactsArray = JSONArray()
        for (vc in contacts) {
            val obj = JSONObject()
            obj.put("name", vc.name)
            obj.put("designation", vc.designation)
            obj.put("company", vc.company)
            obj.put("phone", vc.phone)
            obj.put("email", vc.email)
            obj.put("website", vc.website)
            obj.put("address", vc.address)
            obj.put("meetingNotes", vc.meetingNotes)
            contactsArray.put(obj)
        }
        root.put("contacts", contactsArray)

        return BackupEncryption.encryptBackup(root.toString(), password)
    }

    suspend fun restoreEncryptedBackup(encryptedBytes: ByteArray, password: CharArray): Int {
        val decryptedJson = BackupEncryption.decryptBackup(encryptedBytes, password)
        val root = JSONObject(decryptedJson)

        var restoredCount = 0

        // Restore Cards
        if (root.has("cards")) {
            val cardsArray = root.getJSONArray("cards")
            for (i in 0 until cardsArray.length()) {
                val obj = cardsArray.getJSONObject(i)
                val card = CardEntity(
                    name = obj.getString("name"),
                    category = obj.getString("category"),
                    customCategoryName = obj.optString("customCategoryName", null),
                    cardholderName = obj.optString("cardholderName", null),
                    cardNumber = obj.optString("cardNumber", null),
                    issuingBank = obj.optString("issuingBank", null),
                    issueDate = obj.optString("issueDate", null),
                    expiryDate = obj.optString("expiryDate", null),
                    contactNumber = obj.optString("contactNumber", null),
                    website = obj.optString("website", null),
                    address = obj.optString("address", null),
                    colorHex = obj.optString("colorHex", "#1E3A8A"),
                    cardType = obj.optString("cardType", "DEBIT"),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    notes = obj.optString("notes", null),
                    isTrackingEnabled = obj.optBoolean("isTrackingEnabled", false),
                    openingBalance = obj.optDouble("openingBalance", 0.0),
                    currency = obj.optString("currency", "PKR")
                )
                cardDao.insertCard(card)
                restoredCount++
            }
        }

        // Restore Family
        if (root.has("family")) {
            val familyArray = root.getJSONArray("family")
            for (i in 0 until familyArray.length()) {
                val obj = familyArray.getJSONObject(i)
                familyMemberDao.insertFamilyMember(
                    FamilyMemberEntity(
                        name = obj.getString("name"),
                        relationship = obj.getString("relationship"),
                        phone = obj.optString("phone", null),
                        email = obj.optString("email", null),
                        notes = obj.optString("notes", null),
                        avatarColorHex = obj.optString("avatarColorHex", "#10B981")
                    )
                )
            }
        }

        // Restore Transactions
        if (root.has("transactions")) {
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        currency = obj.optString("currency", "PKR"),
                        category = obj.getString("category"),
                        description = obj.getString("description"),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        notes = obj.optString("notes", null)
                    )
                )
            }
        }

        // Restore Notes
        if (root.has("notes")) {
            val notesArray = root.getJSONArray("notes")
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                noteDao.insertNote(
                    NoteEntity(
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        hasReminder = obj.optBoolean("hasReminder", false),
                        reminderTime = if (obj.has("reminderTime")) obj.getLong("reminderTime") else null
                    )
                )
            }
        }

        // Restore Contacts
        if (root.has("contacts")) {
            val contactsArray = root.getJSONArray("contacts")
            for (i in 0 until contactsArray.length()) {
                val obj = contactsArray.getJSONObject(i)
                visitingContactDao.insertContact(
                    VisitingContactEntity(
                        name = obj.getString("name"),
                        designation = obj.optString("designation", null),
                        company = obj.optString("company", null),
                        phone = obj.optString("phone", null),
                        email = obj.optString("email", null),
                        website = obj.optString("website", null),
                        address = obj.optString("address", null),
                        meetingNotes = obj.optString("meetingNotes", null)
                    )
                )
            }
        }

        activityLogDao.insertLog(
            ActivityLogEntity(
                action = "BACKUP_RESTORED",
                entityType = "VAULT",
                entityId = 0,
                description = "Restored backup with $restoredCount cards and records."
            )
        )

        return restoredCount
    }
}
