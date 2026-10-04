package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Deletes every merchant that no transaction uses, to clear out leftovers in one go. Merchants in
 * use are never touched. Returns how many were deleted.
 */
class DeleteUnusedMerchantsUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    suspend operator fun invoke(): Int {
        val usage = repository.getUsageCounts().first()
        val unused = repository.getAll().first().filter { (usage[it.id] ?: 0) == 0 }
        unused.forEach { repository.delete(it) }
        return unused.size
    }
}
