package com.example.personalfinances.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalfinances.data.local.db.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the "expenses" table.
 *
 * All query methods return [Flow], so the UI layer receives live updates whenever the
 * underlying data changes without needing to manually re-fetch.
 */
@Dao
interface ExpenseDao {

    /** Emits all expenses ordered by date descending, updating on any change. */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    /** Emits expenses whose date falls within [monthStart] (inclusive) to [monthEnd] (exclusive). */
    @Query("""
        SELECT * FROM expenses
        WHERE date >= :monthStart AND date < :monthEnd
        ORDER BY date DESC
    """)
    fun getExpensesByMonth(monthStart: Long, monthEnd: Long): Flow<List<ExpenseEntity>>

    /**
     * Emits the sum of [ExpenseEntity.amount] for rows whose [ExpenseEntity.type] is in [types]
     * and whose [ExpenseEntity.date] is on or before [upToDate]. Returns 0.0 when no rows match.
     *
     * The [upToDate] cutoff ensures future recurring entries are not counted until their month
     * arrives. Pass [System.currentTimeMillis] as the cutoff for a "today and earlier" total.
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE type IN (:types) AND date <= :upToDate")
    fun getTotalByTypes(types: List<String>, upToDate: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE recurring_group_id = :groupId AND date >= :fromDate")
    suspend fun deleteExpenseSeriesFromDate(groupId: String, fromDate: Long)

    @Query("""
        UPDATE expenses
        SET amount = :amount, title = :title, description = :description,
            type = :type, is_recurring = :isRecurring, cadence_months = :cadenceMonths
        WHERE recurring_group_id = :groupId AND date >= :fromDate
    """)
    suspend fun updateExpenseSeriesFromDate(
        groupId: String,
        fromDate: Long,
        amount: Double,
        title: String,
        description: String,
        type: String,
        isRecurring: Boolean,
        cadenceMonths: Int
    )
}
