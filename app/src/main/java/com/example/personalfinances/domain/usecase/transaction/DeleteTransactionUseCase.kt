package com.example.personalfinances.domain.usecase.transaction

import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) = repository.delete(transaction)
}
