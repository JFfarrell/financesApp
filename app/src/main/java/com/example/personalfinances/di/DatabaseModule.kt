package com.example.personalfinances.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.personalfinances.data.local.db.AppDatabase
import com.example.personalfinances.data.local.db.Migrations
import com.example.personalfinances.data.local.db.dao.BackupDao
import com.example.personalfinances.data.local.db.dao.CategoryDao
import com.example.personalfinances.data.local.db.dao.MerchantDao
import com.example.personalfinances.data.local.db.dao.SavingsGoalDao
import com.example.personalfinances.data.local.db.dao.TransactionDao
import com.example.personalfinances.domain.model.enums.TransactionType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and its DAOs as singletons.
 *
 * There is deliberately no destructive-migration fallback: if the schema version changes without
 * a migration, Room refuses to open the database instead of wiping the user's data. Every schema
 * change therefore needs a migration (see the "DB changes" notes in CLAUDE.md).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Starter categories inserted when the database is first created, keyed by transaction type. */
    private val defaultCategories = mapOf(
        TransactionType.EXPENSE to listOf(
            "Housing", "Groceries", "Transport", "Utilities", "Health",
            "Entertainment", "Dining Out", "Shopping", "Other"
        ),
        TransactionType.INCOME to listOf("Salary", "Bonus", "Investments", "Other"),
        TransactionType.SAVING to listOf("Emergency Fund", "Retirement", "General Savings", "Other")
    )

    /**
     * Seeds [defaultCategories] on database creation (including re-creation after a destructive
     * migration). Uses raw SQL because the DAOs are not available until the database is built.
     */
    private object SeedCategoriesCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            defaultCategories.forEach { (type, names) ->
                names.forEach { name ->
                    db.execSQL(
                        "INSERT INTO categories (id, name, transaction_type) VALUES (?, ?, ?)",
                        arrayOf(UUID.randomUUID().toString(), name, type.name)
                    )
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "personal_finances.db")
            .addMigrations(Migrations.MIGRATION_9_10)
            .addCallback(SeedCategoriesCallback)
            .build()

    @Provides
    fun provideSavingsGoalDao(db: AppDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideMerchantDao(db: AppDatabase): MerchantDao = db.merchantDao()

    @Provides
    fun provideBackupDao(db: AppDatabase): BackupDao = db.backupDao()
}
