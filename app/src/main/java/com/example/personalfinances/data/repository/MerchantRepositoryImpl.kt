package com.example.personalfinances.data.repository

import androidx.room.withTransaction
import com.example.personalfinances.data.local.db.AppDatabase
import com.example.personalfinances.data.local.db.dao.MerchantDao
import com.example.personalfinances.data.local.db.dao.TransactionDao
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Room-backed [MerchantRepository]; converts between [Merchant] and its Room entity. It needs the
 * [AppDatabase] itself only to wrap a merge (an update plus a delete) in one transaction.
 */
class MerchantRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val merchantDao: MerchantDao,
    private val transactionDao: TransactionDao
) : MerchantRepository {

    override fun getAll(): Flow<List<Merchant>> =
        merchantDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(merchantId: String): Flow<Merchant?> =
        merchantDao.getById(merchantId).map { it?.toDomain() }

    override suspend fun add(merchant: Merchant) =
        merchantDao.insertMerchant(merchant.toEntity())

    override fun getUsageCounts(): Flow<Map<String, Int>> =
        transactionDao.getMerchantUsage().map { rows -> rows.associate { it.id to it.count } }

    override fun getMerchantLastUsedDates(): Flow<Map<String, LocalDate>> =
        transactionDao.getMerchantLastUsed(LocalDate.now())
            .map { rows -> rows.associate { it.id to it.lastUsed } }

    override fun getFrequentMerchantIds(since: LocalDate, limit: Int): Flow<List<String>> =
        transactionDao.getFrequentMerchantUsage(since, LocalDate.now(), limit)
            .map { rows -> rows.map { it.id } }

    override suspend fun update(merchant: Merchant) =
        merchantDao.updateMerchant(merchant.toEntity())

    override suspend fun delete(merchant: Merchant) =
        merchantDao.deleteMerchant(merchant.toEntity())

    override suspend fun mergeMerchants(source: Merchant, target: Merchant) {
        database.withTransaction {
            transactionDao.reassignMerchant(source.id, target.id)
            merchantDao.deleteMerchant(source.toEntity())
        }
    }
}
