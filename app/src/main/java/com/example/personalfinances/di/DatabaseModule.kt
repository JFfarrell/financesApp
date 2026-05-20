package com.example.personalfinances.di

import android.content.Context
import androidx.room.Room
import com.example.personalfinances.data.local.db.AppDatabase
import com.example.personalfinances.data.local.db.dao.ExpenseDao
import com.example.personalfinances.data.local.db.dao.IncomeDao
import com.example.personalfinances.data.local.db.dao.SavingsGoalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and its DAOs as singletons.
 *
 * [fallbackToDestructiveMigration] means Room will drop and recreate the database when the
 * schema version changes and no explicit migration is provided. This is acceptable during
 * development but should be replaced with proper migrations before any production release.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "personal_finances.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideIncomeDao(db: AppDatabase): IncomeDao = db.incomeDao()

    @Provides
    fun provideSavingsGoalDao(db: AppDatabase): SavingsGoalDao = db.savingsGoalDao()
}
