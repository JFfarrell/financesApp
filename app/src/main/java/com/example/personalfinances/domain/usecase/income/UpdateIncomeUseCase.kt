package com.example.personalfinances.domain.usecase.income

import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.repository.IncomeRepository
import javax.inject.Inject

/** Updates an existing income record, matched by [Income.id]. */
class UpdateIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    suspend operator fun invoke(income: Income) = repository.updateIncome(income)
}
