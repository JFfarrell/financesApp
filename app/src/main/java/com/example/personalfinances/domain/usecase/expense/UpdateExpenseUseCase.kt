package com.example.personalfinances.domain.usecase.expense

import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.repository.ExpenseRepository
import javax.inject.Inject

/** Updates an existing expense record, matched by [Expense.id]. */
class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) = repository.updateExpense(expense)
}
