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
     * Emits the running sum of [Expense.amount] for all entries whose type name is in [types].
     * Emits 0.0 when no matching expenses exist.
     *
     * Used by the savings screen to derive the current saved total from
     * [com.example.personalfinances.domain.model.ExpenseType.SAVINGS_MONTHLY] and
     * [com.example.personalfinances.domain.model.ExpenseType.SAVINGS_EXTRA] entries.
     */
    fun getTotalByTypes(types: List<String>): Flow<Double>

    suspend fun addExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpenseSeries(groupId: String, fromDate: Long)
    suspend fun updateExpenseSeries(expense: Expense)
}
