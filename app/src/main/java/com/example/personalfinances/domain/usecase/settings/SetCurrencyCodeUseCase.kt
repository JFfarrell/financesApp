package com.example.personalfinances.domain.usecase.settings

import com.example.personalfinances.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Saves the user's chosen currency code, or clears it (null) to follow the phone's currency.
 * This only changes how amounts are displayed: stored amounts are plain numbers and are never
 * converted.
 */
class SetCurrencyCodeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(code: String?) = repository.saveCurrencyCode(code)
}
