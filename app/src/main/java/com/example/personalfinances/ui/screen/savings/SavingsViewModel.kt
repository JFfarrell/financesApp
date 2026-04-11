package com.example.personalfinances.ui.screen.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.SavingsGoal
import com.example.personalfinances.domain.usecase.savings.GetSavingsGoalUseCase
import com.example.personalfinances.domain.usecase.savings.UpdateSavingsGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingsUiState(
    val goal: SavingsGoal = SavingsGoal(targetAmount = 0.0, currentSaved = 0.0),
    val isEditingTarget: Boolean = false,
    val isEditingSaved: Boolean = false,
    val isLoading: Boolean = true
)

sealed class SavingsEvent {
    data class UpdateTarget(val newTarget: Double) : SavingsEvent()
    data class UpdateCurrentSaved(val amount: Double) : SavingsEvent()
    object ShowEditTarget : SavingsEvent()
    object HideEditTarget : SavingsEvent()
    object ShowEditSaved : SavingsEvent()
    object HideEditSaved : SavingsEvent()
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val getSavingsGoalUseCase: GetSavingsGoalUseCase,
    private val updateSavingsGoalUseCase: UpdateSavingsGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSavingsGoalUseCase().collect { goal ->
                _uiState.update {
                    it.copy(
                        goal = goal ?: SavingsGoal(0.0, 0.0),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onEvent(event: SavingsEvent) {
        when (event) {
            is SavingsEvent.UpdateTarget -> viewModelScope.launch {
                updateSavingsGoalUseCase(
                    _uiState.value.goal.copy(targetAmount = event.newTarget)
                )
                _uiState.update { it.copy(isEditingTarget = false) }
            }
            is SavingsEvent.UpdateCurrentSaved -> viewModelScope.launch {
                updateSavingsGoalUseCase(
                    _uiState.value.goal.copy(currentSaved = event.amount)
                )
                _uiState.update { it.copy(isEditingSaved = false) }
            }
            SavingsEvent.ShowEditTarget -> _uiState.update { it.copy(isEditingTarget = true) }
            SavingsEvent.HideEditTarget -> _uiState.update { it.copy(isEditingTarget = false) }
            SavingsEvent.ShowEditSaved -> _uiState.update { it.copy(isEditingSaved = true) }
            SavingsEvent.HideEditSaved -> _uiState.update { it.copy(isEditingSaved = false) }
        }
    }
}
