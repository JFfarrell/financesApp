package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Emits the merchants the user has entered most often lately, most frequent first, for one-tap
 * suggestions in the add sheet. "Lately" is the last [WINDOW_DAYS] days, so the suggestions follow
 * current habits rather than all of history. Recurring entries are ignored (see the repository).
 */
class GetFrequentMerchantsUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<Merchant>> =
        combine(
            repository.getAll(),
            repository.getFrequentMerchantIds(LocalDate.now().minusDays(WINDOW_DAYS), limit)
        ) { merchants, frequentIds ->
            val byId = merchants.associateBy { it.id }
            frequentIds.mapNotNull { byId[it] }
        }

    companion object {
        const val WINDOW_DAYS = 90L
        const val DEFAULT_LIMIT = 6
    }
}
