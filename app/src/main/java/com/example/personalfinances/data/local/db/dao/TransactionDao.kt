package com.example.personalfinances.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalfinances.data.local.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAll() : Flow<List<TransactionEntity>>

    @Query("SELECT MIN(date) FROM transactions")
    fun getFirstTransactionDate(): Flow<LocalDate?>

    @Query("SELECT MAX(date) FROM transactions")
    fun getLastTransactionDate(): Flow<LocalDate?>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun getById(transactionId: String) : Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end")
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE merchant_id = :merchantId")
    fun getByMerchant(merchantId: String) : Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId")
    fun getByCategory(categoryId: String) : Flow<List<TransactionEntity>>

    /** Number of transactions per category, for the categories that have any. */
    @Query("SELECT category_id AS id, COUNT(*) AS count FROM transactions GROUP BY category_id")
    fun getCategoryUsage(): Flow<List<UsageCount>>

    /** Number of transactions per merchant, for the merchants that have any. */
    @Query("""SELECT merchant_id AS id, COUNT(*) AS count FROM transactions
            WHERE merchant_id IS NOT NULL GROUP BY merchant_id""")
    fun getMerchantUsage(): Flow<List<UsageCount>>

    /**
     * The date of the latest transaction per merchant, ignoring entries after [today] (future
     * entries of a recurring series are created in advance and have not really "happened" yet).
     */
    @Query("""SELECT merchant_id AS id, MAX(date) AS lastUsed FROM transactions
            WHERE merchant_id IS NOT NULL AND date <= :today GROUP BY merchant_id""")
    fun getMerchantLastUsed(today: LocalDate): Flow<List<MerchantLastUsed>>

    /**
     * The merchants entered most often between [since] and [today], most frequent first (ties go
     * to the most recent), at most [limit] of them. Recurring entries are left out: they are
     * generated automatically, so they say nothing about what the user picks by hand.
     */
    @Query("""SELECT merchant_id AS id, COUNT(*) AS count FROM transactions
            WHERE merchant_id IS NOT NULL AND is_recurring = 0 AND date BETWEEN :since AND :today
            GROUP BY merchant_id ORDER BY count DESC, MAX(date) DESC LIMIT :limit""")
    fun getFrequentMerchantUsage(since: LocalDate, today: LocalDate, limit: Int): Flow<List<UsageCount>>

    /** Points every transaction of merchant [fromId] at merchant [toId] instead. */
    @Query("UPDATE transactions SET merchant_id = :toId WHERE merchant_id = :fromId")
    suspend fun reassignMerchant(fromId: String, toId: String)

    /** Every transaction in a recurring series dated on or after [fromDate]. */
    @Query("SELECT * FROM transactions WHERE recurring_group_id = :groupId AND date >= :fromDate")
    suspend fun getSeriesFromDate(groupId: String, fromDate: LocalDate): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("""SELECT COALESCE(SUM(amount), 0.0) 
            FROM transactions 
            WHERE merchant_id = :merchantId
            AND (:transactionType IS NULL OR :transactionType = transaction_type)
            """)
    fun getTotalByMerchant(merchantId: String, transactionType: String?) : Flow<Double>

    @Query("""SELECT COALESCE(SUM(amount), 0.0)
            FROM transactions
            WHERE transaction_type = :transactionType AND date <= :upToDate
            """)
    fun getTotalByType(transactionType: String, upToDate: LocalDate) : Flow<Double>

    @Query("""SELECT COALESCE(SUM(amount), 0.0) 
            FROM transactions
            WHERE category_id = :categoryId 
            AND (:transactionType IS NULL OR :transactionType = transaction_type)
            """)
    fun getTotalByCategory(categoryId: String, transactionType: String?) : Flow<Double>

    @Query("DELETE FROM transactions WHERE recurring_group_id = :groupId AND date >= :fromDate")
    suspend fun deleteTransactionSeriesFromDate(groupId: String, fromDate: LocalDate)

    @Query("""
        UPDATE transactions
        SET transaction_type = :transactionType, 
            amount = :amount,
            cadence_unit = :cadenceUnit,
            cadence_value = :cadenceValue,
            category_id = :categoryId,
            merchant_id = :merchantId,
            is_recurring = :isRecurring,
            notes = :notes,
            tags = :tags
        WHERE recurring_group_id = :groupId AND date >= :fromDate
    """)
    suspend fun updateTransactionSeriesFromDate(
        transactionType: String,
        groupId: String,
        fromDate: LocalDate,
        amount: Double,
        cadenceUnit: String,
        cadenceValue: Int,
        categoryId: String,
        merchantId: String?,
        isRecurring: Boolean,
        notes: String?,
        tags: String
    )
}