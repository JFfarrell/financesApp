package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Returns transactions between [start] and [end], both inclusive. The caller supplies the range
 * so it can honour the user's pay-cycle start day rather than assuming calendar months.
 */
class GetTransactionsByMonthUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        repository.getByDateRange(start, end)
}
