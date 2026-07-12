package com.example.personalfinances.domain.usecase.settings

import com.example.personalfinances.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Persists the pay-cycle start day. [day] must be in the range 1–28.
 */
class SetPayCycleStartDayUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(day: Int) = repository.savePayCycleStartDay(day.coerceIn(1, 28))
}
