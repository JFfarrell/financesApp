package com.example.personalfinances.domain.usecase.expense

import com.example.personalfinances.domain.repository.ExpenseRepository
import javax.inject.Inject

/** Deletes all expenses sharing [groupId] whose date is on or after [fromDate]. */
class DeleteExpenseSeriesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(groupId: String, fromDate: Long) =
        repository.deleteExpenseSeries(groupId, fromDate)
}
