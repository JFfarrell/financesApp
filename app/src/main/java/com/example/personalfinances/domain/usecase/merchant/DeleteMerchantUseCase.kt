package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Deletes a merchant, but only if no transaction uses it. The database would allow deleting one
 * in use (those transactions would just lose their merchant), but that silently discards
 * information, so it is refused and the user can rename the merchant instead.
 */
class DeleteMerchantUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    suspend operator fun invoke(merchant: Merchant): OperationResult {
        val used = repository.getUsageCounts().first()[merchant.id] ?: 0
        if (used > 0) {
            val noun = if (used == 1) "transaction" else "transactions"
            return OperationResult.Failure("\"${merchant.name}\" is used by $used $noun. Rename it instead.")
        }
        return try {
            repository.delete(merchant)
            OperationResult.Success
        } catch (e: Exception) {
            OperationResult.Failure("Could not delete \"${merchant.name}\".")
        }
    }
}
