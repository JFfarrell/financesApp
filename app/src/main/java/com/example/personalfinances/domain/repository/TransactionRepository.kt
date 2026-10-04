package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getById(id: String): Flow<Transaction?>
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<Transaction>>
    fun getByMerchant(merchantId: String): Flow<List<Transaction>>
    fun getByCategory(categoryId: String): Flow<List<Transaction>>
    fun getTotalByMerchant(merchantId: String, transactionType: TransactionType?): Flow<Double>
    fun getTotalByCategory(categoryId: String, transactionType: TransactionType?): Flow<Double>
    /** Sums all transactions of [transactionType] dated on or before [upToDate]. */
    fun getTotalByType(transactionType: TransactionType, upToDate: LocalDate): Flow<Double>
    fun getFirstTransactionDate(): Flow<LocalDate?>
    fun getLastTransactionDate(): Flow<LocalDate?>
    suspend fun add(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    /** Every transaction in a recurring series dated on or after [date], for example to undo a delete. */
    suspend fun getSeriesFromDate(recurringGroupId: String, date: LocalDate): List<Transaction>
    suspend fun deleteSeriesFromDate(recurringGroupId: String, date: LocalDate)
    suspend fun updateTransactionSeriesFromDate(date: LocalDate, transaction: Transaction)
}