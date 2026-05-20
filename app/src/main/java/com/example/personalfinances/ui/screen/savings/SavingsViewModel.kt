package com.example.personalfinances.ui.screen.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.SavingsGoal
import com.example.personalfinances.domain.usecase.savings.GetSavingsGoalUseCase
import com.example.personalfinances.domain.usecase.savings.GetSavingsTotalUseCase
import com.example.personalfinances.domain.usecase.savings.UpdateSavingsGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds all UI state for the Savings screen.
 *
 * The key responsibility here is deriving [SavingsUiState.currentSaved] reactively. Rather than
 * trusting a manually-entered value, it is computed as:
 *   currentSaved = goal.startingAmount + savingsExpensesTotal
 *
 * This is achieved by combining two flows with [combine]: the savings goal from the database, and
 * the running sum of all SAVINGS-type expenses. Whenever either changes — e.g. the user adds a
 * savings expense elsewhere in the app — the savings screen updates automatically.
 */
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val getSavingsGoalUseCase: GetSavingsGoalUseCase,
    private val getSavingsTotalUseCase: GetSavingsTotalUseCase,
    private val updateSavingsGoalUseCase: UpdateSavingsGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // combine() merges two flows into one. Every time either the goal or the expense
            // total emits a new value, the lambda runs and produces a fresh UI state.
            combine(
                getSavingsGoalUseCase(),
                getSavingsTotalUseCase()
            ) { goal, savingsTotal ->
                val resolvedGoal = goal ?: SavingsGoal(targetAmount = 0.0, startingAmount = 0.0)
                val currentSaved = resolvedGoal.startingAmount + savingsTotal
                Triple(resolvedGoal, currentSaved, savingsTotal)
            }.collect { (goal, currentSaved, _) ->
                _uiState.update {
                    it.copy(goal = goal, currentSaved = currentSaved, isLoading = false)
                }
            }
        }
    }

    /** Processes a user action from the Savings screen. */
    fun onEvent(event: SavingsEvent) {
        when (event) {
            is SavingsEvent.UpdateTarget -> viewModelScope.launch {
                updateSavingsGoalUseCase(
                    _uiState.value.goal.copy(targetAmount = event.newTarget)
                )
                _uiState.update { it.copy(isEditingTarget = false) }
            }
            is SavingsEvent.UpdateStartingAmount -> viewModelScope.launch {
                updateSavingsGoalUseCase(
                    _uiState.value.goal.copy(startingAmount = event.amount)
                )
                _uiState.update { it.copy(isEditingStartingAmount = false) }
            }
            SavingsEvent.ShowEditTarget -> _uiState.update { it.copy(isEditingTarget = true) }
            SavingsEvent.HideEditTarget -> _uiState.update { it.copy(isEditingTarget = false) }
            SavingsEvent.ShowEditStartingAmount -> _uiState.update { it.copy(isEditingStartingAmount = true) }
            SavingsEvent.HideEditStartingAmount -> _uiState.update { it.copy(isEditingStartingAmount = false) }
        }
    }
}

/**
 * Immutable snapshot of the Savings screen's UI state.
 *
 * [currentSaved] is computed — not stored — as: goal.startingAmount + savings expenses total.
 * [isEditingStartingAmount] drives the dialog for setting the pre-app savings seed value.
 */
data class SavingsUiState(
    val goal: SavingsGoal = SavingsGoal(targetAmount = 0.0, startingAmount = 0.0),
    val currentSaved: Double = 0.0,
    val isEditingTarget: Boolean = false,
    val isEditingStartingAmount: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * All actions a user can take on the Savings screen.
 */
sealed class SavingsEvent {
    /** Updates the savings target amount. */
    data class UpdateTarget(val newTarget: Double) : SavingsEvent()

    /**
     * Updates the starting amount — the pre-app savings seed.
     * This is a one-time setup value, not the ongoing tracked total.
     */
    data class UpdateStartingAmount(val amount: Double) : SavingsEvent()

    object ShowEditTarget : SavingsEvent()
    object HideEditTarget : SavingsEvent()
    object ShowEditStartingAmount : SavingsEvent()
    object HideEditStartingAmount : SavingsEvent()
}
