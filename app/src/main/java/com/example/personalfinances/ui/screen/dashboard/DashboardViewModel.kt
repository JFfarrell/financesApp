package com.example.personalfinances.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.usecase.savings.GetSavingsGoalUseCase
import com.example.personalfinances.domain.usecase.settings.GetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.transaction.GetSavingsTotalUseCase
import com.example.personalfinances.domain.usecase.transaction.GetTransactionsByMonthUseCase
import com.example.personalfinances.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Holds all UI state for the Home (dashboard) screen.
 *
 * Loads the month's transactions reactively and summarises them into income, spending, savings
 * and a per-category breakdown. When the user navigates between months, the previous month's
 * collection job is cancelled and a new one started, preventing stale data from leaking across
 * navigations. It also tracks the savings goal progress shown on Home. Settings and backup live
 * in their own view models.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val getPayCycleStartDayUseCase: GetPayCycleStartDayUseCase,
    private val getSavingsGoalUseCase: GetSavingsGoalUseCase,
    private val getSavingsTotalUseCase: GetSavingsTotalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var monthJob: Job? = null

    init {
        getPayCycleStartDayUseCase().onEach { startDay ->
            _uiState.update { it.copy(payCycleStartDay = startDay) }
            loadMonth(_uiState.value.selectedMonth)
        }.launchIn(viewModelScope)

        // Goal progress = starting amount + every SAVING transaction up to today.
        combine(getSavingsGoalUseCase(), getSavingsTotalUseCase()) { goal, savingsTotal ->
            (goal?.targetAmount ?: 0.0) to ((goal?.startingAmount ?: 0.0) + savingsTotal)
        }.onEach { (target, saved) ->
            _uiState.update { it.copy(savingsTarget = target, savingsCurrent = saved) }
        }.launchIn(viewModelScope)
    }

    private fun loadMonth(month: YearMonth) {
        val startDay = _uiState.value.payCycleStartDay
        monthJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedMonth = month) }
        monthJob = viewModelScope.launch {
            val (start, end) = DateUtils.monthDateRange(month, startDay)
            summariseMonth(start, end)
        }
    }

    /** Collects the transactions between [start] and [end] and publishes their summary. */
    private suspend fun summariseMonth(start: LocalDate, end: LocalDate) {
        getTransactionsByMonthUseCase(start, end)
            .map { summarise(it) }
            .collect { summary ->
                _uiState.update {
                    it.copy(
                        totalIncome = summary.income,
                        totalSpent = summary.spent,
                        totalSaved = summary.saved,
                        remainder = summary.income - summary.spent - summary.saved,
                        expensesByCategory = summary.byCategory,
                        isLoading = false
                    )
                }
            }
    }

    /** Processes a user action from the Home screen. */
    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.PreviousMonth -> loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            DashboardEvent.NextMonth -> loadMonth(_uiState.value.selectedMonth.plusMonths(1))
        }
    }
}

/** Totals for one month, computed from its transactions. */
private data class MonthSummary(
    val income: Double,
    val spent: Double,
    val saved: Double,
    val byCategory: Map<String, Double>
)

/**
 * Splits [transactions] into income, spending and savings totals, and groups everything that
 * left the balance (spending and savings) by category name. Savings are counted with spending in
 * the breakdown, as they always have been, so the remainder is income minus everything that went out.
 */
private fun summarise(transactions: List<Transaction>): MonthSummary {
    fun total(type: TransactionType) =
        transactions.filter { it.transactionType == type }.sumOf { it.amount }

    val wentOut = transactions.filter { it.transactionType != TransactionType.INCOME }
    return MonthSummary(
        income = total(TransactionType.INCOME),
        spent = total(TransactionType.EXPENSE),
        saved = total(TransactionType.SAVING),
        byCategory = wentOut
            .groupBy { it.category.name }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    )
}

/**
 * Immutable snapshot of the Home screen's UI state.
 *
 * [remainder] is income minus spending minus savings. [expensesByCategory] maps each category
 * name to the total that went out under it (savings included) for the selected month.
 * [savingsCurrent] is the goal's starting amount plus all savings to date; [savingsTarget] is 0
 * when no goal has been set.
 */
data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val payCycleStartDay: Int = 1,
    val totalIncome: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalSaved: Double = 0.0,
    val remainder: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val savingsTarget: Double = 0.0,
    val savingsCurrent: Double = 0.0,
    val isLoading: Boolean = true
)

/** User actions on the Home screen. */
sealed class DashboardEvent {
    object PreviousMonth : DashboardEvent()
    object NextMonth : DashboardEvent()
}
