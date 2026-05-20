package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for expense data operations.
 *
 * Abstracts the data source from the domain layer — use cases and ViewModels depend on this
 * interface, not on the Room implementation directly.
 */
interface ExpenseRepository {
    /** Emits the full list of expenses, ordered by date descending, updating on any change. */
    fun getAllExpenses(): Flow<List<Expense>>

    /** Emits expenses whose date falls within [monthStart] (inclusive) to [monthEnd] (exclusive). */
    fun getExpensesByMonth(monthStart: Long, monthEnd: Long): Flow<List<Expense>>

    /**
     * Emits the running sum of [Expense.amount] for entries whose type name is in [types] and
     * whose date is on or before [upToDate]. Emits 0.0 when no matching expenses exist.
     *
     * [upToDate] is an epoch-millis cutoff that prevents future recurring entries from being
     * counted before their month arrives.
     */
    fun getTotalByTypes(types: List<String>, upToDate: Long): Flow<Double>

    suspend fun addExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpenseSeries(groupId: String, fromDate: Long)
    suspend fun updateExpenseSeries(expense: Expense)
}
