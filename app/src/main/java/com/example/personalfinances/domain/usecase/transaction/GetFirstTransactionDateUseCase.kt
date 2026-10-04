package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/** Emits the date of the earliest transaction, or null when there are none yet. */
class GetFirstTransactionDateUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<LocalDate?> = repository.getFirstTransactionDate()
}
