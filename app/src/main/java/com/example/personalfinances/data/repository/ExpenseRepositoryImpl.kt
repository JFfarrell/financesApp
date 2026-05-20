package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.db.dao.ExpenseDao
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed implementation of [ExpenseRepository].
 *
 * Delegates all data operations to [ExpenseDao] and maps between [ExpenseEntity] and [Expense]
 * using the mapper extension functions. The domain layer never sees Room entities directly.
 */
class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> =
        dao.getAllExpenses().map { list -> list.map { it.toDomain() } }

    override fun getExpensesByMonth(monthStart: Long, monthEnd: Long): Flow<List<Expense>> =
        dao.getExpensesByMonth(monthStart, monthEnd).map { list -> list.map { it.toDomain() } }

    override fun getTotalByTypes(types: List<String>): Flow<Double> =
        dao.getTotalByTypes(types)

    override suspend fun addExpense(expense: Expense) {
        dao.insertExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpenseSeries(groupId: String, fromDate: Long) {
        dao.deleteExpenseSeriesFromDate(groupId, fromDate)
    }

    override suspend fun updateExpenseSeries(expense: Expense) {
        dao.updateExpenseSeriesFromDate(
            groupId = expense.recurringGroupId!!,
            fromDate = expense.date,
            amount = expense.amount,
            title = expense.title,
            description = expense.description,
            type = expense.type.name,
            isRecurring = expense.isRecurring,
            cadenceMonths = expense.cadenceMonths
        )
    }
}
