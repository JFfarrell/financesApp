package com.example.personalfinances.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.domain.usecase.settings.GetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity-level state: the saved [ThemeMode], read before any screen draws so the whole app can
 * be themed. Starts as [ThemeMode.SYSTEM] until the saved value loads.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
