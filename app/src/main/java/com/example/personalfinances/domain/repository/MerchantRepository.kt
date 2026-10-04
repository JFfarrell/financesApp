package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Merchant
import kotlinx.coroutines.flow.Flow

interface MerchantRepository {
    fun getAll(): Flow<List<Merchant>>
    fun getById(merchantId: String): Flow<Merchant?>
    suspend fun add(merchant: Merchant)

    /** Emits, for each merchant id that has transactions, how many transactions use it. */
    fun getUsageCounts(): Flow<Map<String, Int>>

    /** Saves changes to an existing merchant (for example a new name). */
    suspend fun update(merchant: Merchant)

    /** Removes a merchant. Transactions that used it keep existing, without a merchant. */
    suspend fun delete(merchant: Merchant)
}
