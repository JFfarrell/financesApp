package com.example.personalfinances.di

import com.example.personalfinances.data.repository.AuthRepositoryImpl
import com.example.personalfinances.data.repository.BackupRepositoryImpl
import com.example.personalfinances.data.repository.CategoryRepositoryImpl
import com.example.personalfinances.data.repository.MerchantRepositoryImpl
import com.example.personalfinances.data.repository.SavingsGoalRepositoryImpl
import com.example.personalfinances.data.repository.SettingsRepositoryImpl
import com.example.personalfinances.data.repository.TransactionRepositoryImpl
import com.example.personalfinances.domain.repository.AuthRepository
import com.example.personalfinances.domain.repository.BackupRepository
import com.example.personalfinances.domain.repository.CategoryRepository
import com.example.personalfinances.domain.repository.MerchantRepository
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import com.example.personalfinances.domain.repository.SettingsRepository
import com.example.personalfinances.domain.repository.TransactionRepository
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
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindMerchantRepository(impl: MerchantRepositoryImpl): MerchantRepository

    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
