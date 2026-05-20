package com.example.personalfinances.domain.usecase.income

import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.repository.IncomeRepository
import javax.inject.Inject

/**
 * Updates all income entries in the same recurring series as [income] whose start date is on or
 * after [income.startDate]. Each entry's individual start date is preserved.
 */
class UpdateIncomeSeriesUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    suspend operator fun invoke(income: Income) = repository.updateIncomeSeries(income)
}
