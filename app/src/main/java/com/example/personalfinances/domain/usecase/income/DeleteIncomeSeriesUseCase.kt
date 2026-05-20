package com.example.personalfinances.domain.usecase.income

import com.example.personalfinances.domain.repository.IncomeRepository
import javax.inject.Inject

/** Deletes all income entries sharing [groupId] whose start date is on or after [fromDate]. */
class DeleteIncomeSeriesUseCase @Inject constructor(
    private val repository: IncomeRepository
) {
    suspend operator fun invoke(groupId: String, fromDate: Long) =
        repository.deleteIncomeSeries(groupId, fromDate)
}
