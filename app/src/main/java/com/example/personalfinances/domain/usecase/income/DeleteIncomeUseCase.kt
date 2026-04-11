package com.example.personalfinances.domain.usecase.income

import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.repository.IncomeRepository
import javax.inject.Inject

class DeleteIncomeUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    suspend operator fun invoke(income: Income) = repository.deleteIncome(income)
}
