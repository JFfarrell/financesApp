package com.example.personalfinances.ui.screen.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.usecase.expense.AddExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.DeleteExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.GetExpensesByMonthUseCase
import com.example.personalfinances.domain.usecase.expense.UpdateExpenseUseCase
import com.example.personalfinances.domain.usecase.income.AddIncomeUseCase
import com.example.personalfinances.domain.usecase.income.DeleteIncomeUseCase
import com.example.personalfinances.domain.usecase.income.GetIncomesUseCase
import com.example.personalfinances.domain.usecase.income.UpdateIncomeUseCase
import com.example.personalfinances.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * Immutable snapshot of the Calendar screen's UI state.
 *
 * [expenseSheetTarget] is null when the expense sheet is in Add mode, or holds the expense
 * being edited. Same pattern for [incomeSheetTarget].
 */
data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val expenses: List<Expense> = emptyList(),
    val recurringExpenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val isLoading: Boolean = true,
    val isExpenseSheetOpen: Boolean = false,
    val expenseSheetTarget: Expense? = null,
    val isIncomeSheetOpen: Boolean = false,
    val incomeSheetTarget: Income? = null
)

/** All actions a user can take on the Calendar screen. */
sealed class CalendarEvent {
    object PreviousMonth : CalendarEvent()
    object NextMonth : CalendarEvent()

    data class AddExpense(val expense: Expense) : CalendarEvent()
    data class UpdateExpense(val expense: Expense) : CalendarEvent()
    data class DeleteExpense(val expense: Expense) : CalendarEvent()
    object ShowAddExpenseSheet : CalendarEvent()
    data class ShowEditExpenseSheet(val expense: Expense) : CalendarEvent()
    object HideExpenseSheet : CalendarEvent()

    data class AddIncome(val income: Income) : CalendarEvent()
    data class UpdateIncome(val income: Income) : CalendarEvent()
    data class DeleteIncome(val income: Income) : CalendarEvent()
    object ShowAddIncomeSheet : CalendarEvent()
    data class ShowEditIncomeSheet(val income: Income) : CalendarEvent()
    object HideIncomeSheet : CalendarEvent()
}

/**
 * Holds all UI state for the Calendar screen and handles user-driven events.
 *
 * Both expense and income flows for the selected month are combined into a single collector so
 * both lists update atomically and [CalendarUiState.isLoading] clears only once both are ready.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getExpensesByMonthUseCase: GetExpensesByMonthUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val updateIncomeUseCase: UpdateIncomeUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var monthJob: Job? = null

    init {
        loadMonth(YearMonth.now())
    }

    private fun loadMonth(month: YearMonth) {
        monthJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedMonth = month) }
        monthJob = viewModelScope.launch {
            val (start, end) = DateUtils.monthBounds(month)
            combine(
                getExpensesByMonthUseCase(start, end),
                getIncomesUseCase(start, end)
            ) { expenses, incomes -> expenses to incomes }
                .collect { (expenses, incomes) ->
                    _uiState.update {
                        it.copy(
                            expenses = expenses.filter { e -> !e.isRecurring },
                            recurringExpenses = expenses.filter { e -> e.isRecurring },
                            incomes = incomes,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /** Processes a user action from the Calendar screen. */
    fun onEvent(event: CalendarEvent) {
        when (event) {
            CalendarEvent.PreviousMonth ->
                loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            CalendarEvent.NextMonth ->
                loadMonth(_uiState.value.selectedMonth.plusMonths(1))

            is CalendarEvent.AddExpense -> viewModelScope.launch {
                addExpenseUseCase(event.expense)
                _uiState.update { it.copy(isExpenseSheetOpen = false, expenseSheetTarget = null) }
            }
            is CalendarEvent.UpdateExpense -> viewModelScope.launch {
                updateExpenseUseCase(event.expense)
                _uiState.update { it.copy(isExpenseSheetOpen = false, expenseSheetTarget = null) }
            }
            is CalendarEvent.DeleteExpense -> viewModelScope.launch {
                deleteExpenseUseCase(event.expense)
            }
            CalendarEvent.ShowAddExpenseSheet ->
                _uiState.update { it.copy(isExpenseSheetOpen = true, expenseSheetTarget = null) }
            is CalendarEvent.ShowEditExpenseSheet ->
                _uiState.update { it.copy(isExpenseSheetOpen = true, expenseSheetTarget = event.expense) }
            CalendarEvent.HideExpenseSheet ->
                _uiState.update { it.copy(isExpenseSheetOpen = false, expenseSheetTarget = null) }

            is CalendarEvent.AddIncome -> viewModelScope.launch {
                addIncomeUseCase(event.income)
                _uiState.update { it.copy(isIncomeSheetOpen = false, incomeSheetTarget = null) }
            }
            is CalendarEvent.UpdateIncome -> viewModelScope.launch {
                updateIncomeUseCase(event.income)
                _uiState.update { it.copy(isIncomeSheetOpen = false, incomeSheetTarget = null) }
            }
            is CalendarEvent.DeleteIncome -> viewModelScope.launch {
                deleteIncomeUseCase(event.income)
            }
            CalendarEvent.ShowAddIncomeSheet ->
                _uiState.update { it.copy(isIncomeSheetOpen = true, incomeSheetTarget = null) }
            is CalendarEvent.ShowEditIncomeSheet ->
                _uiState.update { it.copy(isIncomeSheetOpen = true, incomeSheetTarget = event.income) }
            CalendarEvent.HideIncomeSheet ->
                _uiState.update { it.copy(isIncomeSheetOpen = false, incomeSheetTarget = null) }
        }
    }
}
