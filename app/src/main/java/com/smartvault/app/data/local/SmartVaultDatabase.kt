package com.smartvault.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartvault.app.data.local.converters.Converters
import com.smartvault.app.data.local.dao.*
import com.smartvault.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CardEntity::class,
        CategoryEntity::class,
        FamilyMemberEntity::class,
        TransactionEntity::class,
        NoteEntity::class,
        VisitingContactEntity::class,
        ActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartVaultDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao
    abstract fun visitingContactDao(): VisitingContactDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: SmartVaultDatabase? = null

        fun getDatabase(context: Context): SmartVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartVaultDatabase::class.java,
                    "smart_vault.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Prepopulate default system categories and Self family member
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        val defaultCategories = listOf(
                            CategoryEntity(name = "My Personal Cards", iconName = "person", colorHex = "#3B82F6", isSystem = true, sortOrder = 1),
                            CategoryEntity(name = "Bank & Payment Cards", iconName = "account_balance", colorHex = "#10B981", isSystem = true, sortOrder = 2),
                            CategoryEntity(name = "Family Members' Cards", iconName = "groups", colorHex = "#8B5CF6", isSystem = true, sortOrder = 3),
                            CategoryEntity(name = "Visiting & Business Cards", iconName = "badge", colorHex = "#F59E0B", isSystem = true, sortOrder = 4),
                            CategoryEntity(name = "Identity & Membership Cards", iconName = "card_membership", colorHex = "#EC4899", isSystem = true, sortOrder = 5),
                            CategoryEntity(name = "Other Custom Cards", iconName = "more_horiz", colorHex = "#64748B", isSystem = true, sortOrder = 6)
                        )
                        defaultCategories.forEach { database.categoryDao().insertCategory(it) }

                        // Default family profile: Self
                        database.familyMemberDao().insertFamilyMember(
                            FamilyMemberEntity(
                                name = "My Profile (Self)",
                                relationship = "Self",
                                avatarColorHex = "#6366F1",
                                notes = "Primary account holder profile"
                            )
                        )
                    }
                }
            }
        }
    }
}
