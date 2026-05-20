package com.example.personalfinances.ui.screen.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.model.ExpenseType
import com.example.personalfinances.domain.usecase.expense.AddExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.DeleteExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.GetExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds all UI state for the Expenses screen and handles user-driven events.
 *
 * State is exposed as a [StateFlow] of [ExpensesUiState]. Events are funnelled through a single
 * [onEvent] function, keeping the composable free of business logic.
 */
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getExpensesUseCase().collect { expenses ->
                _uiState.update { it.copy(expenses = expenses, isLoading = false) }
            }
        }
    }

    /** Processes a user action from the Expenses screen. */
    fun onEvent(event: ExpensesEvent) {
        when (event) {
            is ExpensesEvent.AddExpense -> viewModelScope.launch {
                addExpenseUseCase(
                    Expense(
                        amount = event.amount,
                        title = event.title,
                        description = event.description,
                        type = event.type,
                        date = event.date,
                        isRecurring = event.isRecurring,
                        cadenceMonths = event.cadenceMonths
                    )
                )
                _uiState.update { it.copy(isAddSheetVisible = false) }
            }
            is ExpensesEvent.DeleteExpense -> viewModelScope.launch {
                deleteExpenseUseCase(event.expense)
            }
            ExpensesEvent.ShowAddSheet -> _uiState.update { it.copy(isAddSheetVisible = true) }
            ExpensesEvent.HideAddSheet -> _uiState.update { it.copy(isAddSheetVisible = false) }
        }
    }
}

/**
 * Immutable snapshot of the Expenses screen's UI state.
 *
 * [isLoading] is true until the first emission from the database flow arrives.
 */
data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val isAddSheetVisible: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * All actions a user can take on the Expenses screen.
 */
sealed class ExpensesEvent {
    /**
     * Fired when the user taps Save in the add-expense sheet.
     *
     * [title] is the required short label. [description] is only required when
     * [ExpenseType.isDescriptionEditable] is true (i.e. `*_OTHER` types).
     */
    data class AddExpense(
        val amount: Double,
        val title: String,
        val description: String,
        val type: ExpenseType,
        val date: Long,
        val isRecurring: Boolean,
        val cadenceMonths: Int
    ) : ExpensesEvent()

    /** Fired when the user deletes an expense. */
    data class DeleteExpense(val expense: Expense) : ExpensesEvent()

    /** Opens the add-expense bottom sheet. */
    object ShowAddSheet : ExpensesEvent()

    /** Closes the add-expense bottom sheet without saving. */
    object HideAddSheet : ExpensesEvent()
}
