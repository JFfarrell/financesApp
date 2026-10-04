package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.repository.TransactionRepository
import java.time.LocalDate
import javax.inject.Inject

class DeleteTransactionSeriesUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(recurringGroupId: String, fromDate: LocalDate) =
        repository.deleteSeriesFromDate(recurringGroupId, fromDate)
}
