package com.example.personalfinances.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.domain.usecase.settings.GetCurrencyCodeUseCase
import com.example.personalfinances.domain.usecase.settings.GetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity-level state read before any screen draws so the whole app can be themed: the saved
 * [ThemeMode] and the chosen currency code. They start as [ThemeMode.SYSTEM] and no chosen
 * currency until the saved values load.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    getCurrencyCodeUseCase: GetCurrencyCodeUseCase
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** The chosen ISO 4217 currency code, or null to follow the phone's currency. */
    val currencyCode: StateFlow<String?> = getCurrencyCodeUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
