package com.example.personalfinances.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
 *
 * [fallbackToDestructiveMigration] is set in [com.example.personalfinances.di.DatabaseModule],
 * so no explicit migration SQL is needed during development — the database is recreated on
 * version bumps. This should be replaced with proper migrations before shipping.
 */
@TypeConverters(Converters::class)
@Database(
    entities = [
        SavingsGoalEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        TransactionEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantDao(): MerchantDao
    abstract fun categoryDao(): CategoryDao

}
