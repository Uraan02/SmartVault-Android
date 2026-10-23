package com.smartvault.app.data.local.dao

import androidx.room.*
import com.smartvault.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY relationship = 'Self' DESC, name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getFamilyMemberById(id: Long): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    fun getFamilyMemberByIdFlow(id: Long): Flow<FamilyMemberEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMemberEntity)

    @Query("SELECT * FROM family_members")
    suspend fun getAllForExport(): List<FamilyMemberEntity>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    fun getTransactionsByCard(cardId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    suspend fun getTransactionsByCardList(cardId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE familyMemberId = :familyMemberId ORDER BY date DESC")
    fun getTransactionsByFamilyMember(familyMemberId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE LOWER(description) LIKE '%' || LOWER(:query) || '%' OR 
              LOWER(category) LIKE '%' || LOWER(:query) || '%'
        ORDER BY date DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllForExport(): List<TransactionEntity>
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE cardId = :cardId AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByCard(cardId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE familyMemberId = :familyMemberId AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFamilyMember(familyMemberId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE contactId = :contactId AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByContact(contactId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE hasReminder = 1 AND isArchived = 0 ORDER BY reminderTime ASC")
    fun getUpcomingReminders(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE isArchived = 0 AND (
            LOWER(title) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(content) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM notes")
    suspend fun getAllForExport(): List<NoteEntity>
}

@Dao
interface VisitingContactDao {
    @Query("SELECT * FROM visiting_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<VisitingContactEntity>>

    @Query("SELECT * FROM visiting_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): VisitingContactEntity?

    @Query("SELECT * FROM visiting_contacts WHERE id = :id LIMIT 1")
    fun getContactByIdFlow(id: Long): Flow<VisitingContactEntity?>

    @Query("SELECT * FROM visiting_contacts WHERE LOWER(phone) = LOWER(:phone) AND id != :excludeId LIMIT 1")
    suspend fun findDuplicateByPhone(phone: String, excludeId: Long = 0): VisitingContactEntity?

    @Query("""
        SELECT * FROM visiting_contacts 
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR 
              LOWER(company) LIKE '%' || LOWER(:query) || '%' OR 
              LOWER(phone) LIKE '%' || LOWER(:query) || '%' OR 
              LOWER(email) LIKE '%' || LOWER(:query) || '%' OR
              LOWER(designation) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name ASC
    """)
    fun searchContacts(query: String): Flow<List<VisitingContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: VisitingContactEntity): Long

    @Update
    suspend fun updateContact(contact: VisitingContactEntity)

    @Delete
    suspend fun deleteContact(contact: VisitingContactEntity)

    @Query("DELETE FROM visiting_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("SELECT * FROM visiting_contacts ORDER BY name ASC")
    suspend fun getAllForExport(): List<VisitingContactEntity>
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivities(limit: Int = 30): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE entityType = :type AND entityId = :id ORDER BY timestamp DESC")
    fun getActivitiesForEntity(type: String, id: Long): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insertLog(log: ActivityLogEntity): Long
}
