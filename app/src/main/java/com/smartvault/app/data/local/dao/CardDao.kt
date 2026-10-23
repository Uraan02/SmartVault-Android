package com.smartvault.app.data.local.dao

import androidx.room.*
import com.smartvault.app.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE isArchived = 0 ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllActiveCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE category = :category AND isArchived = 0 ORDER BY isFavorite DESC, updatedAt DESC")
    fun getCardsByCategory(category: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE isFavorite = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getFavoriteCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE familyMemberId = :familyMemberId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getCardsByFamilyMember(familyMemberId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE isTrackingEnabled = 1 AND isArchived = 0")
    fun getTrackedCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    fun getCardByIdFlow(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE cardNumber = :cardNumber AND id != :excludeId LIMIT 1")
    suspend fun findDuplicateCardByNumber(cardNumber: String, excludeId: Long = 0): CardEntity?

    @Query("SELECT * FROM cards WHERE LOWER(name) = LOWER(:name) AND id != :excludeId LIMIT 1")
    suspend fun findDuplicateCardByName(name: String, excludeId: Long = 0): CardEntity?

    @Query("""
        SELECT * FROM cards 
        WHERE isArchived = 0 AND (
            LOWER(name) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(cardholderName) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(issuingBank) LIKE '%' || LOWER(:query) || '%' OR 
            LOWER(cardNumber) LIKE '%' || LOWER(:query) || '%' OR
            LOWER(notes) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY updatedAt DESC
    """)
    fun searchCards(query: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("SELECT COUNT(*) FROM cards WHERE isArchived = 0")
    fun getActiveCardsCount(): Flow<Int>

    @Query("SELECT * FROM cards")
    suspend fun getAllCardsForExport(): List<CardEntity>
}
