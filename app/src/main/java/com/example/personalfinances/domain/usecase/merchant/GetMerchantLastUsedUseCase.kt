package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Emits the date each merchant was last used (by merchant id), updating as transactions change.
 * Merchants with no transaction yet, or only future ones, are absent from the map.
 */
class GetMerchantLastUsedUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    operator fun invoke(): Flow<Map<String, LocalDate>> = repository.getMerchantLastUsedDates()
}
