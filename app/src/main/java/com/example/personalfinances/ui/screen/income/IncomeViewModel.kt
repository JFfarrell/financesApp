package com.example.personalfinances.ui.screen.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.model.IncomeType
import com.example.personalfinances.domain.usecase.income.AddIncomeUseCase
import com.example.personalfinances.domain.usecase.income.DeleteIncomeUseCase
import com.example.personalfinances.domain.usecase.income.GetIncomesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds all UI state for the Income screen and handles user-driven events.
 *
 * State is exposed as a [StateFlow] of [IncomeUiState] so the composable can observe it
 * reactively. Events are funnelled through a single [onEvent] function, keeping the composable
 * free of business logic.
 */
@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val getIncomesUseCase: GetIncomesUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeUiState())
    val uiState: StateFlow<IncomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getIncomesUseCase().collect { incomes ->
                _uiState.update { it.copy(incomes = incomes, isLoading = false) }
            }
        }
    }

    /** Processes a user action from the Income screen. */
    fun onEvent(event: IncomeEvent) {
        when (event) {
            is IncomeEvent.AddIncome -> viewModelScope.launch {
                addIncomeUseCase(
                    Income(
                        amount = event.amount,
                        type = event.type,
                        description = event.description,
                        cadenceMonths = event.cadenceMonths,
                        startDate = event.startDate
                    )
                )
                _uiState.update { it.copy(isAddSheetVisible = false) }
            }
            is IncomeEvent.DeleteIncome -> viewModelScope.launch {
                deleteIncomeUseCase(event.income)
            }
            IncomeEvent.ShowAddSheet -> _uiState.update { it.copy(isAddSheetVisible = true) }
            IncomeEvent.HideAddSheet -> _uiState.update { it.copy(isAddSheetVisible = false) }
        }
    }
}

/**
 * Immutable snapshot of the Income screen's UI state.
 *
 * [isLoading] is true until the first emission from the database flow arrives.
 * [isAddSheetVisible] drives whether the add-income bottom sheet is shown.
 */
data class IncomeUiState(
    val incomes: List<Income> = emptyList(),
    val isAddSheetVisible: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * All actions a user can take on the Income screen.
 *
 * Using a sealed class ensures the ViewModel's [IncomeViewModel.onEvent] handler is exhaustive —
 * the compiler will warn if a new event type is added but not handled.
 */
sealed class IncomeEvent {
    /**
     * Fired when the user taps Save in the add-income sheet.
     *
     * [description] is only populated when [type] is [IncomeType.OTHER]; it is null otherwise.
     */
    data class AddIncome(
        val amount: Double,
        val type: IncomeType,
        val description: String?,
        val cadenceMonths: Int,
        val startDate: Long
    ) : IncomeEvent()

    /** Fired when the user swipes to delete or taps the delete icon on an income item. */
    data class DeleteIncome(val income: Income) : IncomeEvent()

    /** Opens the add-income bottom sheet. */
    object ShowAddSheet : IncomeEvent()

    /** Closes the add-income bottom sheet without saving. */
    object HideAddSheet : IncomeEvent()
}
