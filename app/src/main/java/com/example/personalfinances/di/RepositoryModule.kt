package com.example.personalfinances.di

import com.example.personalfinances.data.repository.AuthRepositoryImpl
import com.example.personalfinances.data.repository.ExpenseRepositoryImpl
import com.example.personalfinances.data.repository.IncomeRepositoryImpl
import com.example.personalfinances.data.repository.SavingsGoalRepositoryImpl
import com.example.personalfinances.domain.repository.AuthRepository
import com.example.personalfinances.domain.repository.ExpenseRepository
import com.example.personalfinances.domain.repository.IncomeRepository
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their Room-backed implementations.
 *
 * Using [Binds] (rather than [dagger.Provides]) is more efficient — Hilt generates a direct
 * delegation without an extra wrapper class.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository

    @Binds @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
