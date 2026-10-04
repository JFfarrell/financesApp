package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Renames a merchant. Every transaction using it shows the new name straight away.
 *
 * Refused if the name is blank or another merchant already has that name (ignoring case).
 */
class RenameMerchantUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    suspend operator fun invoke(merchant: Merchant, newName: String): OperationResult {
        val name = newName.trim()
        if (name.isEmpty()) return OperationResult.Failure("The name can't be empty.")
        if (name == merchant.name) return OperationResult.Success

        val clash = repository.getAll().first().any {
            it.id != merchant.id && it.name.equals(name, ignoreCase = true)
        }
        if (clash) return OperationResult.Failure("There is already a merchant called \"$name\".")

        repository.update(merchant.copy(name = name))
        return OperationResult.Success
    }
}
