package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.repository.TransactionRepository
import java.time.LocalDate
import javax.inject.Inject

class UpdateTransactionSeriesUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(fromDate: LocalDate, transaction: Transaction) =
        repository.updateTransactionSeriesFromDate(fromDate, transaction)
}
