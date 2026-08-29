package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.db.dao.CategoryDao
import com.example.personalfinances.data.local.db.dao.MerchantDao
import com.example.personalfinances.data.local.db.dao.TransactionDao
import com.example.personalfinances.data.local.db.entity.TransactionEntity
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val merchantDao: MerchantDao

) : TransactionRepository {
    override fun getAll(): Flow<List<Transaction>> {
        return transactionDao.getAll().map { entities ->
            entities.map { mapEntity(it) }
        }
    }

    override fun getById(id: String): Flow<Transaction?> {
        return transactionDao.getById(id).map { entity ->
            entity?.let {
                mapEntity(entity)
            }
        }
    }

    override fun getByDateRange(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<Transaction>> {
        return transactionDao.getByDateRange(start, end).map { entities ->
            entities.map { mapEntity(it) }
        }
    }

    override fun getByMerchant(merchantId: String): Flow<List<Transaction>> {
        return transactionDao.getByMerchant(merchantId).map { entities ->
            entities.map { mapEntity(it) }
        }
    }

    override fun getByCategory(categoryId: String): Flow<List<Transaction>> {
        return transactionDao.getByCategory(categoryId).map { entities ->
            entities.map { mapEntity(it) }
        }
    }

    override fun getTotalByMerchant(merchantId: String,
                                    transactionType: TransactionType?): Flow<Double> {
        return transactionDao.getTotalByMerchant(merchantId, transactionType?.name)
    }

    override fun getTotalByCategory(
        categoryId: String,
        transactionType: TransactionType?
    ): Flow<Double> {
        return transactionDao.getTotalByCategory(categoryId, transactionType?.name)

    }

    override fun getTotalByType(
        transactionType: TransactionType,
        upToDate: LocalDate
    ): Flow<Double> {
        return transactionDao.getTotalByType(transactionType.name, upToDate)
    }

    override fun getFirstTransactionDate(): Flow<LocalDate?> {
        return transactionDao.getFirstTransactionDate()
    }

    override fun getLastTransactionDate(): Flow<LocalDate?> {
        return transactionDao.getLastTransactionDate()
    }

    override suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun deleteSeriesFromDate(
        recurringGroupId: String,
        date: LocalDate
    ) {
        transactionDao.deleteTransactionSeriesFromDate(recurringGroupId, date)
    }

    override suspend fun updateTransactionSeriesFromDate(
        date: LocalDate,
        transaction: Transaction
    ) {
        transactionDao.updateTransactionSeriesFromDate(
            transaction.transactionType.name,
            transaction.recurringGroupId!!,
            date,
            transaction.amount,
            transaction.cadenceUnit.name,
            transaction.cadenceValue,
            transaction.category.id,
            transaction.merchant?.id,
            transaction.isRecurring,
            transaction.notes,
            Json.encodeToString(transaction.tags)
            )
    }

    private suspend fun mapEntity(entity: TransactionEntity): Transaction {
        val category = categoryDao.getById(entity.categoryId).first()!!.toDomain()
        val merchant = entity.merchantId?.let { merchantDao.getById(it).first()?.toDomain() }
        return entity.toDomain(category, merchant)
    }
}