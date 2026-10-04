package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.repository.TransactionRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Returns every transaction in a recurring series dated on or after [fromDate]. Used before a
 * "this and future" delete so the whole set can be restored if the user taps Undo.
 */
class GetTransactionSeriesUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(recurringGroupId: String, fromDate: LocalDate): List<Transaction> =
        repository.getSeriesFromDate(recurringGroupId, fromDate)
}
