package com.example.personalfinances.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.usecase.auth.IsPasswordSetUseCase
import com.example.personalfinances.domain.usecase.auth.SetPasswordUseCase
import com.example.personalfinances.domain.usecase.auth.VerifyPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val isPasswordSetUseCase: IsPasswordSetUseCase,
    private val setPasswordUseCase: SetPasswordUseCase,
    private val verifyPasswordUseCase: VerifyPasswordUseCase
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object FirstTimeSetup : UiState()
        object Login : UiState()
        object Authenticated : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isSet = isPasswordSetUseCase().first()
            _uiState.value = if (isSet) UiState.Login else UiState.FirstTimeSetup
        }
    }

    fun onSubmitPassword(password: String) {
        viewModelScope.launch {
            when (_uiState.value) {
                is UiState.FirstTimeSetup -> {
                    setPasswordUseCase(password)
                    _uiState.value = UiState.Authenticated
                }
                is UiState.Login -> {
                    val success = verifyPasswordUseCase(password)
                    _uiState.value = if (success) UiState.Authenticated
                                     else UiState.Error("Incorrect password. Try again.")
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Login
        }
    }
}
