package com.example.personalfinances.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.data.local.db.entity.MerchantEntity
import com.example.personalfinances.data.local.db.entity.SavingsGoalEntity
import com.example.personalfinances.data.local.db.entity.TransactionEntity

/**
 * Bulk reads and writes used only by backup and restore.
 *
 * The reads return plain lists (not Flows) because a backup is a one-off snapshot. The writes use
 * `@Upsert`, which inserts new rows and updates existing ones in place. The normal DAOs use
 * REPLACE, which deletes then inserts and would trip the foreign key from transactions to
 * categories during a restore.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM categories")
    suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM merchants")
    suspend fun merchants(): List<MerchantEntity>

    @Query("SELECT * FROM transactions")
    suspend fun transactions(): List<TransactionEntity>

    @Query("SELECT * FROM savings_goals WHERE id = 1 LIMIT 1")
    suspend fun savingsGoal(): SavingsGoalEntity?

    // The four queries below exist only to signal that something changed. Room re-runs a query, and
    // so re-emits, whenever its table is written, even if the result is the same, which is what the
    // automatic backup relies on. The first emission on subscribing is just the current state.
    @Query("SELECT COUNT(*) FROM transactions")
    fun transactionChanges(): Flow<Int>

    @Query("SELECT COUNT(*) FROM categories")
    fun categoryChanges(): Flow<Int>

    @Query("SELECT COUNT(*) FROM merchants")
    fun merchantChanges(): Flow<Int>

    @Query("SELECT COUNT(*) FROM savings_goals")
    fun savingsGoalChanges(): Flow<Int>

    @Upsert
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Upsert
    suspend fun upsertMerchants(items: List<MerchantEntity>)

    @Upsert
    suspend fun upsertTransactions(items: List<TransactionEntity>)

    @Upsert
    suspend fun upsertSavingsGoal(goal: SavingsGoalEntity)
}
