package com.example.personalfinances.domain.usecase.savings

import com.example.personalfinances.domain.model.ExpenseType
import com.example.personalfinances.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a [Flow] that emits the running total of all savings expenses.
 *
 * Both [ExpenseType.SAVINGS_MONTHLY] and [ExpenseType.SAVINGS_EXTRA] entries contribute to the
 * total. The result is used by the savings screen to compute "current saved" as:
 *   startingAmount + savingsTotal
 *
 * The flow updates automatically whenever a savings expense is added or deleted.
 */
class GetSavingsTotalUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<Double> = repository.getTotalByTypes(
        listOf(ExpenseType.SAVINGS_MONTHLY.name, ExpenseType.SAVINGS_EXTRA.name)
    )
}
