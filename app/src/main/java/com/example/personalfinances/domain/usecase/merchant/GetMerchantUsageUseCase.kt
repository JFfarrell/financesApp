package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits how many transactions use each merchant (by merchant id), updating as they change. */
class GetMerchantUsageUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    operator fun invoke(): Flow<Map<String, Int>> = repository.getUsageCounts()
}
