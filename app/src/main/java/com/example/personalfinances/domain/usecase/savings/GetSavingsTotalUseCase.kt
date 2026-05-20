package com.example.personalfinances.domain.usecase.savings

import com.example.personalfinances.domain.model.ExpenseType
import com.example.personalfinances.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a [Flow] that emits the running total of savings expenses up to and including today.
 *
 * Both [ExpenseType.SAVINGS_MONTHLY] and [ExpenseType.SAVINGS_EXTRA] entries contribute to the
 * total. The result is used by the savings screen to compute "current saved" as:
 *   startingAmount + savingsTotal
 *
 * Passing [System.currentTimeMillis] as the cutoff means future recurring entries are excluded
 * until their month arrives — adding 6 months of recurring savings does not front-load the total.
 */
class GetSavingsTotalUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<Double> = repository.getTotalByTypes(
        types = listOf(ExpenseType.SAVINGS_MONTHLY.name, ExpenseType.SAVINGS_EXTRA.name),
        upToDate = System.currentTimeMillis()
    )
}
