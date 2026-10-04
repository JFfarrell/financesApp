package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Merchant
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MerchantRepository {
    fun getAll(): Flow<List<Merchant>>
    fun getById(merchantId: String): Flow<Merchant?>
    suspend fun add(merchant: Merchant)

    /** Emits, for each merchant id that has transactions, how many transactions use it. */
    fun getUsageCounts(): Flow<Map<String, Int>>

    /**
     * Emits, for each merchant id that has transactions, the date of its latest one that is not in
     * the future. Merchants without such a transaction are absent.
     */
    fun getMerchantLastUsedDates(): Flow<Map<String, LocalDate>>

    /**
     * Emits the ids of the merchants entered by hand most often since [since] (recurring entries
     * do not count), most frequent first, at most [limit] of them.
     */
    fun getFrequentMerchantIds(since: LocalDate, limit: Int): Flow<List<String>>

    /** Saves changes to an existing merchant (for example a new name). */
    suspend fun update(merchant: Merchant)

    /** Removes a merchant. Transactions that used it keep existing, without a merchant. */
    suspend fun delete(merchant: Merchant)

    /**
     * Moves every transaction of [source] to [target], then deletes [source]. Both steps happen in
     * one database transaction, so a failure part-way leaves everything as it was.
     */
    suspend fun mergeMerchants(source: Merchant, target: Merchant)
}
