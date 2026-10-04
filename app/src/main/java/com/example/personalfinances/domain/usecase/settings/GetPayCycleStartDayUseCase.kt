package com.example.personalfinances.domain.usecase.settings

import com.example.personalfinances.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns a [Flow] that emits the configured pay-cycle start day (1–28).
 *
 * Emits 1 (calendar-month behaviour) until the user explicitly sets a different value.
 */
class GetPayCycleStartDayUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Int> = repository.getPayCycleStartDay()
}
