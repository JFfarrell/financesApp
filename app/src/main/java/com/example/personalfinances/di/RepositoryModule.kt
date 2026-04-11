package com.example.personalfinances.di

import com.example.personalfinances.data.repository.AuthRepositoryImpl
import com.example.personalfinances.data.repository.CategoryRepositoryImpl
import com.example.personalfinances.data.repository.ExpenseRepositoryImpl
import com.example.personalfinances.data.repository.IncomeRepositoryImpl
import com.example.personalfinances.data.repository.SavingsGoalRepositoryImpl
import com.example.personalfinances.domain.repository.AuthRepository
import com.example.personalfinances.domain.repository.CategoryRepository
import com.example.personalfinances.domain.repository.ExpenseRepository
import com.example.personalfinances.domain.repository.IncomeRepository
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
