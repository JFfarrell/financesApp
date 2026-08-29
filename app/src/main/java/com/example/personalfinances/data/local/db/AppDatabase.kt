package com.example.personalfinances.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.personalfinances.data.local.db.dao.CategoryDao
import com.example.personalfinances.data.local.db.dao.ExpenseDao
import com.example.personalfinances.data.local.db.dao.IncomeDao
import com.example.personalfinances.data.local.db.dao.MerchantDao
import com.example.personalfinances.data.local.db.dao.SavingsGoalDao
import com.example.personalfinances.data.local.db.dao.TransactionDao
import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.data.local.db.entity.MerchantEntity
import com.example.personalfinances.data.local.db.entity.TransactionEntity
import com.example.personalfinances.data.local.db.entity.legacy.ExpenseEntity
import com.example.personalfinances.data.local.db.entity.legacy.IncomeEntity
import com.example.personalfinances.data.local.db.entity.legacy.SavingsGoalEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
 *
 * [fallbackToDestructiveMigration] is set in [com.example.personalfinances.di.DatabaseModule],
 * so no explicit migration SQL is needed during development — the database is recreated on
 * version bumps. This should be replaced with proper migrations before shipping.
 */
@TypeConverters(AppDatabase::class)
@Database(
    entities = [
        ExpenseEntity::class,
        IncomeEntity::class,
        SavingsGoalEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        TransactionEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantDao(): MerchantDao
    abstract fun categoryDao(): CategoryDao


    @TypeConverter
    fun longToLocalDate(value: Long?): LocalDate? =
        value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? =
        date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

}
