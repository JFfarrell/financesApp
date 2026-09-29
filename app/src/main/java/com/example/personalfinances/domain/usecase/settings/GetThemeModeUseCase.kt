package com.example.personalfinances.domain.usecase.settings

import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits the user's chosen [ThemeMode], or [ThemeMode.SYSTEM] until they pick one. */
class GetThemeModeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.getThemeMode()
}
