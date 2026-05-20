package com.example.personalfinances.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.personalfinances.data.local.db.dao.ExpenseDao
import com.example.personalfinances.data.local.db.dao.IncomeDao
import com.example.personalfinances.data.local.db.dao.SavingsGoalDao
import com.example.personalfinances.data.local.db.entity.ExpenseEntity
import com.example.personalfinances.data.local.db.entity.IncomeEntity
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
 *
 * [fallbackToDestructiveMigration] is set in [com.example.personalfinances.di.DatabaseModule],
 * so no explicit migration SQL is needed during development — the database is recreated on
 * version bumps. This should be replaced with proper migrations before shipping.
 */
@Database(
    entities = [
        ExpenseEntity::class,
        IncomeEntity::class,
        SavingsGoalEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsGoalDao(): SavingsGoalDao
}
