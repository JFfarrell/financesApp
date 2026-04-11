package com.example.personalfinances.domain.usecase.income

import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIncomesUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    operator fun invoke(): Flow<List<Income>> = repository.getAllIncomes()
    operator fun invoke(monthStart: Long, monthEnd: Long): Flow<List<Income>> =
        repository.getIncomesByMonth(monthStart, monthEnd)
}
