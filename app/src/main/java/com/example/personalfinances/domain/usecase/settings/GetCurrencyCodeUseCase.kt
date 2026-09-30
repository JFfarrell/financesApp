package com.example.personalfinances.domain.usecase.settings

import com.example.personalfinances.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits the currency code the user chose (such as "EUR"), or null to follow the phone's currency. */
class GetCurrencyCodeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<String?> = repository.getCurrencyCode()
}
