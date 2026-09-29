package com.example.personalfinances.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.personalfinances.data.local.db.dao.BackupDao
import com.example.personalfinances.data.local.db.dao.CategoryDao
import com.example.personalfinances.data.local.db.dao.MerchantDao
import com.example.personalfinances.data.local.db.dao.SavingsGoalDao
import com.example.personalfinances.data.local.db.dao.TransactionDao
import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.data.local.db.entity.MerchantEntity
import com.example.personalfinances.data.local.db.entity.TransactionEntity
import com.example.personalfinances.data.local.db.entity.SavingsGoalEntity

/**
 * Root Room database for the app.
 *
 * Version history:
 *  - 1: Initial schema (ExpenseEntity with categoryId FK, IncomeEntity with source, CategoryEntity)
 *  - 2: IncomeEntity replaced `source: String` with `type: String` + `description: String?`
 *  - 3: ExpenseEntity replaced `categoryId` FK with `type: String`; CategoryEntity removed;
 *       SavingsGoalEntity replaced `currentSaved` with `startingAmount`
 *  - 4: ExpenseEntity added `title: String`
 *  - 5: ExpenseEntity and IncomeEntity added `recurring_group_id: String?`
 *  - 6: IncomeEntity added `is_recurring: Boolean`
 *  - 7: Added unified TransactionEntity, CategoryEntity and MerchantEntity alongside the legacy
 *       tables; LocalDate stored via [Converters]
 *  - 8: CategoryEntity added `transaction_type: String` so each type has its own categories
 *  - 9: Removed the legacy ExpenseEntity and IncomeEntity tables; transactions, categories,
 *       merchants and savings goals remain
 *  - 10: Dates stored as epoch days (time zone independent) instead of epoch milliseconds at
 *        local midnight; no table changes, [Migrations.MIGRATION_9_10] converts existing rows
 *
 * Version 9 was the baseline for real data. Versions 1 to 8 were development-only and were wiped
 * on each change. From version 9 on, every schema change must ship a migration (an `AutoMigration`
 * for simple changes, otherwise a hand-written `Migration`), and the exported schema JSON for
 * every version stays in `app/schemas/` so migrations can be tested. There is no destructive
 * fallback, so a missing migration fails loudly instead of deleting data.
 */
@TypeConverters(Converters::class)
@Database(
    entities = [
        SavingsGoalEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        TransactionEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantDao(): MerchantDao
    abstract fun categoryDao(): CategoryDao
    abstract fun backupDao(): BackupDao

}
