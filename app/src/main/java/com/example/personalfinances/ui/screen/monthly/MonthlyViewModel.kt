package com.example.personalfinances.ui.screen.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.usecase.expense.AddExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.DeleteExpenseSeriesUseCase
import com.example.personalfinances.domain.usecase.expense.DeleteExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.GetExpensesByMonthUseCase
import com.example.personalfinances.domain.usecase.expense.UpdateExpenseSeriesUseCase
import com.example.personalfinances.domain.usecase.expense.UpdateExpenseUseCase
import com.example.personalfinances.domain.usecase.income.AddIncomeUseCase
import com.example.personalfinances.domain.usecase.income.DeleteIncomeSeriesUseCase
import com.example.personalfinances.domain.usecase.income.DeleteIncomeUseCase
import com.example.personalfinances.domain.usecase.income.GetIncomesUseCase
import com.example.personalfinances.domain.usecase.income.UpdateIncomeSeriesUseCase
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
import java.util.UUID
import javax.inject.Inject

enum class RecurringScope { THIS_ONLY, THIS_AND_FUTURE }

/** Tracks whether a recurring-scope dialog is waiting for user input and what action it concerns. */
sealed class RecurringDialogState {
    object None : RecurringDialogState()
    data class PendingDeleteExpense(val expense: Expense) : RecurringDialogState()
    data class PendingDeleteIncome(val income: Income) : RecurringDialogState()
    data class PendingUpdateExpense(val updated: Expense) : RecurringDialogState()
    data class PendingUpdateIncome(val updated: Income) : RecurringDialogState()
}

/**
 * Immutable snapshot of the Calendar screen's UI state.
 *
 * [expenseSheetTarget] is null when the expense sheet is in Add mode, or holds the expense
 * being edited. Same pattern for [incomeSheetTarget].
 *
 * [recurringDialog] is non-None when a recurring-scope prompt is waiting for user input.
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
    val incomeSheetTarget: Income? = null,
    val recurringDialog: RecurringDialogState = RecurringDialogState.None
)

/** All actions a user can take on the Calendar screen. */
sealed class CalendarEvent {
    object PreviousMonth : CalendarEvent()
    object NextMonth : CalendarEvent()

    data class AddExpense(val expense: Expense, val durationMonths: Int = 1) : CalendarEvent()
    data class UpdateExpense(val expense: Expense) : CalendarEvent()
    data class DeleteExpense(val expense: Expense) : CalendarEvent()
    object ShowAddExpenseSheet : CalendarEvent()
    data class ShowEditExpenseSheet(val expense: Expense) : CalendarEvent()
    object HideExpenseSheet : CalendarEvent()

    data class AddIncome(val income: Income, val durationMonths: Int = 1) : CalendarEvent()
    data class UpdateIncome(val income: Income) : CalendarEvent()
    data class DeleteIncome(val income: Income) : CalendarEvent()
    object ShowAddIncomeSheet : CalendarEvent()
    data class ShowEditIncomeSheet(val income: Income) : CalendarEvent()
    object HideIncomeSheet : CalendarEvent()

    data class ConfirmDeleteExpense(val expense: Expense, val scope: RecurringScope) : CalendarEvent()
    data class ConfirmDeleteIncome(val income: Income, val scope: RecurringScope) : CalendarEvent()
    data class ConfirmUpdateExpense(val expense: Expense, val scope: RecurringScope) : CalendarEvent()
    data class ConfirmUpdateIncome(val income: Income, val scope: RecurringScope) : CalendarEvent()
    object DismissRecurringDialog : CalendarEvent()
}

/**
 * Holds all UI state for the Calendar screen and handles user-driven events.
 *
 * Both expense and income flows for the selected month are combined into a single collector so
 * both lists update atomically and [CalendarUiState.isLoading] clears only once both are ready.
 *
 * For recurring entries (those with a [Expense.recurringGroupId]), delete and update operations
 * pause and set [CalendarUiState.recurringDialog] so the UI can ask the user whether to apply the
 * change to just this entry or to this and all future entries in the series.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getExpensesByMonthUseCase: GetExpensesByMonthUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val updateExpenseSeriesUseCase: UpdateExpenseSeriesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val deleteExpenseSeriesUseCase: DeleteExpenseSeriesUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val updateIncomeUseCase: UpdateIncomeUseCase,
    private val updateIncomeSeriesUseCase: UpdateIncomeSeriesUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val deleteIncomeSeriesUseCase: DeleteIncomeSeriesUseCase
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
                val expense = event.expense
                if (expense.isRecurring && event.durationMonths > 1) {
                    val groupId = UUID.randomUUID().toString()
                    val cadence = expense.cadenceMonths.coerceAtLeast(1)
                    repeat(event.durationMonths) { i ->
                        addExpenseUseCase(
                            expense.copy(
                                recurringGroupId = groupId,
                                date = DateUtils.addMonths(expense.date, i * cadence)
                            )
                        )
                    }
                } else {
                    addExpenseUseCase(expense)
                }
                _uiState.update { it.copy(isExpenseSheetOpen = false, expenseSheetTarget = null) }
            }

            is CalendarEvent.UpdateExpense -> {
                if (event.expense.recurringGroupId != null) {
                    _uiState.update {
                        it.copy(
                            isExpenseSheetOpen = false,
                            expenseSheetTarget = null,
                            recurringDialog = RecurringDialogState.PendingUpdateExpense(event.expense)
                        )
                    }
                } else {
                    viewModelScope.launch { updateExpenseUseCase(event.expense) }
                    _uiState.update { it.copy(isExpenseSheetOpen = false, expenseSheetTarget = null) }
                }
            }

            is CalendarEvent.DeleteExpense -> {
                if (event.expense.recurringGroupId != null) {
                    _uiState.update {
                        it.copy(recurringDialog = RecurringDialogState.PendingDeleteExpense(event.expense))
                    }
                } else {
                    viewModelScope.launch { deleteExpenseUseCase(event.expense) }
                }
            }

            is CalendarEvent.ConfirmDeleteExpense -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> deleteExpenseUseCase(event.expense)
                    RecurringScope.THIS_AND_FUTURE -> deleteExpenseSeriesUseCase(
                        event.expense.recurringGroupId!!, event.expense.date
                    )
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            is CalendarEvent.ConfirmUpdateExpense -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> updateExpenseUseCase(event.expense)
                    RecurringScope.THIS_AND_FUTURE -> updateExpenseSeriesUseCase(event.expense)
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            CalendarEvent.ShowAddExpenseSheet ->
                _uiState.update { it.copy(isExpenseSheetOpen = true, expenseSheetTarget = null) }
            is CalendarEvent.ShowEditExpenseSheet ->
                _uiState.update { it.copy(isExpenseSheetOpen = true, expenseSheetTarget = event.expense) }
            CalendarEvent.HideExpenseSheet ->
                _uiState.update { it.copy(isExpenseSheetOpen = false, expenseSheetTarget = null) }

            is CalendarEvent.AddIncome -> viewModelScope.launch {
                val income = event.income
                if (income.isRecurring && event.durationMonths > 1) {
                    val groupId = UUID.randomUUID().toString()
                    val cadence = income.cadenceMonths
                    repeat(event.durationMonths) { i ->
                        addIncomeUseCase(
                            income.copy(
                                recurringGroupId = groupId,
                                startDate = DateUtils.addMonths(income.startDate, i * cadence)
                            )
                        )
                    }
                } else {
                    addIncomeUseCase(income)
                }
                _uiState.update { it.copy(isIncomeSheetOpen = false, incomeSheetTarget = null) }
            }

            is CalendarEvent.UpdateIncome -> {
                if (event.income.recurringGroupId != null) {
                    _uiState.update {
                        it.copy(
                            isIncomeSheetOpen = false,
                            incomeSheetTarget = null,
                            recurringDialog = RecurringDialogState.PendingUpdateIncome(event.income)
                        )
                    }
                } else {
                    viewModelScope.launch { updateIncomeUseCase(event.income) }
                    _uiState.update { it.copy(isIncomeSheetOpen = false, incomeSheetTarget = null) }
                }
            }

            is CalendarEvent.DeleteIncome -> {
                if (event.income.recurringGroupId != null) {
                    _uiState.update {
                        it.copy(recurringDialog = RecurringDialogState.PendingDeleteIncome(event.income))
                    }
                } else {
                    viewModelScope.launch { deleteIncomeUseCase(event.income) }
                }
            }

            is CalendarEvent.ConfirmDeleteIncome -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> deleteIncomeUseCase(event.income)
                    RecurringScope.THIS_AND_FUTURE -> deleteIncomeSeriesUseCase(
                        event.income.recurringGroupId!!, event.income.startDate
                    )
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            is CalendarEvent.ConfirmUpdateIncome -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> updateIncomeUseCase(event.income)
                    RecurringScope.THIS_AND_FUTURE -> updateIncomeSeriesUseCase(event.income)
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            CalendarEvent.ShowAddIncomeSheet ->
                _uiState.update { it.copy(isIncomeSheetOpen = true, incomeSheetTarget = null) }
            is CalendarEvent.ShowEditIncomeSheet ->
                _uiState.update { it.copy(isIncomeSheetOpen = true, incomeSheetTarget = event.income) }
            CalendarEvent.HideIncomeSheet ->
                _uiState.update { it.copy(isIncomeSheetOpen = false, incomeSheetTarget = null) }

            CalendarEvent.DismissRecurringDialog ->
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
        }
    }
}
