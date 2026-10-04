package com.example.personalfinances.domain.usecase.settings

import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.domain.repository.SettingsRepository
import javax.inject.Inject

/** Saves the user's chosen [ThemeMode]. */
class SetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.saveThemeMode(mode)
}
