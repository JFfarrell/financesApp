package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.repository.MerchantRepository
import javax.inject.Inject

/**
 * Merges [source] into [target]: every transaction of [source] is moved to [target] and [source]
 * is deleted. This is how duplicates such as "Tesco" and "TESCO 1234" are tidied, since a merchant
 * in use cannot simply be deleted. It cannot be undone, so the screen asks first.
 *
 * Refused if both are the same merchant.
 */
class MergeMerchantsUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    suspend operator fun invoke(source: Merchant, target: Merchant): OperationResult {
        if (source.id == target.id) return OperationResult.Failure("Pick a different merchant to merge into.")
        return try {
            repository.mergeMerchants(source, target)
            OperationResult.Success
        } catch (e: Exception) {
            OperationResult.Failure("Could not merge \"${source.name}\" into \"${target.name}\".")
        }
    }
}
