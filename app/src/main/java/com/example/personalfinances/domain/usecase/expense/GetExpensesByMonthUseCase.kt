package com.example.personalfinances.domain.usecase.expense

import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesByMonthUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(monthStart: Long, monthEnd: Long): Flow<List<Expense>> =
        repository.getExpensesByMonth(monthStart, monthEnd)
}
