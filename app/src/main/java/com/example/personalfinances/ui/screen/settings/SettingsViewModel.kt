package com.example.personalfinances.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.domain.usecase.settings.GetCurrencyCodeUseCase
import com.example.personalfinances.domain.usecase.settings.GetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.settings.GetThemeModeUseCase
import com.example.personalfinances.domain.usecase.settings.SetCurrencyCodeUseCase
import com.example.personalfinances.domain.usecase.settings.SetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The user's preferences shown on the Settings screen. [currencyCode] is null while following the
 * phone's own currency.
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val payCycleStartDay: Int = 1,
    val currencyCode: String? = null
)

/** User actions on the Settings screen. Each change is saved as soon as it is made. */
sealed class SettingsEvent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent()
    data class SetPayCycleStartDay(val day: Int) : SettingsEvent()

    /** [code] is an ISO 4217 code such as "EUR", or null to follow the phone's currency. */
    data class SetCurrency(val code: String?) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    getPayCycleStartDayUseCase: GetPayCycleStartDayUseCase,
    private val setPayCycleStartDayUseCase: SetPayCycleStartDayUseCase,
    getCurrencyCodeUseCase: GetCurrencyCodeUseCase,
    private val setCurrencyCodeUseCase: SetCurrencyCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        getThemeModeUseCase().onEach { mode ->
            _uiState.update { it.copy(themeMode = mode) }
        }.launchIn(viewModelScope)
        getPayCycleStartDayUseCase().onEach { day ->
            _uiState.update { it.copy(payCycleStartDay = day) }
        }.launchIn(viewModelScope)
        getCurrencyCodeUseCase().onEach { code ->
            _uiState.update { it.copy(currencyCode = code) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.SetThemeMode -> setThemeModeUseCase(event.mode)
                is SettingsEvent.SetPayCycleStartDay -> setPayCycleStartDayUseCase(event.day)
                is SettingsEvent.SetCurrency -> setCurrencyCodeUseCase(event.code)
            }
        }
    }
}
