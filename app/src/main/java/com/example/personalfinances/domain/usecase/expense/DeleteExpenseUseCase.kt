package com.example.personalfinances.domain.usecase.expense

import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.repository.ExpenseRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense) = repository.deleteExpense(expense)
}
