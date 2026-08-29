package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Emits the running total of all [TransactionType.SAVING] transactions dated up to and including
 * today. The savings screen adds this to the goal's starting amount to get "current saved".
 *
 * Using today as the cutoff means future recurring savings entries are excluded until their date
 * arrives, so creating a 6-month series does not front-load the total.
 */
class GetSavingsTotalUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<Double> =
        repository.getTotalByType(TransactionType.SAVING, LocalDate.now())
}
