package com.example.personalfinances.domain.usecase.expense

import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Updates all expenses in the same recurring series as [expense] whose date is on or after
 * [expense.date]. The date of each individual entry is preserved; only the content fields
 * (amount, title, description, type, isRecurring, cadenceMonths) are overwritten.
 */
class UpdateExpenseSeriesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) = repository.updateExpenseSeries(expense)
}
