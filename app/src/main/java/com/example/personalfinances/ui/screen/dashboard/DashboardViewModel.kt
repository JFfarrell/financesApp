package com.example.personalfinances.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.usecase.expense.GetExpensesByMonthUseCase
import com.example.personalfinances.domain.usecase.income.GetIncomesUseCase
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
 * Holds all UI state for the Dashboard screen.
 *
 * Loads income and expense data for the selected month reactively. When the user navigates
 * between months, the previous month's collection job is cancelled and a new one started,
 * preventing stale data from leaking across navigations.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getExpensesByMonthUseCase: GetExpensesByMonthUseCase,
    private val getIncomesUseCase: GetIncomesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

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
            ) { expenses, incomes ->
                val totalExpenses = expenses.sumOf { it.amount }
                val totalIncome = incomes.sumOf { it.amount }
                // Group expenses by their type's display name (e.g. "Fuel / Petrol") now that
                // categories are replaced by the predefined ExpenseType enum.
                val byCategory = expenses
                    .groupBy { it.type.displayName }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                Triple(totalExpenses, totalIncome, byCategory)
            }.collect { (totalExpenses, totalIncome, byCategory) ->
                _uiState.update {
                    it.copy(
                        totalExpenses = totalExpenses,
                        totalIncome = totalIncome,
                        remainder = totalIncome - totalExpenses,
                        expensesByCategory = byCategory,
                        isLoading = false
                    )
                }
            }
        }
    }

    /** Processes a navigation event from the Dashboard screen. */
    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.PreviousMonth -> loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            DashboardEvent.NextMonth -> loadMonth(_uiState.value.selectedMonth.plusMonths(1))
        }
    }
}

/**
 * Immutable snapshot of the Dashboard screen's UI state.
 *
 * [expensesByCategory] maps each [com.example.personalfinances.domain.model.ExpenseType.displayName]
 * to the total amount spent under that type for the selected month.
 */
data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainder: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true
)

/**
 * Navigation events for the Dashboard screen.
 */
sealed class DashboardEvent {
    object PreviousMonth : DashboardEvent()
    object NextMonth : DashboardEvent()
}
