package com.example.personalfinances.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.usecase.settings.GetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.settings.SetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.transaction.GetTransactionsByMonthUseCase
import com.example.personalfinances.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Holds all UI state for the Dashboard screen.
 *
 * Loads income and expense (including savings) data for the selected month reactively.
 * When the user navigates between months, the previous month's collection job is
 * cancelled and a new one started, preventing stale data from leaking across navigations.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val getPayCycleStartDayUseCase: GetPayCycleStartDayUseCase,
    private val setPayCycleStartDayUseCase: SetPayCycleStartDayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var monthJob: Job? = null

    init {
        getPayCycleStartDayUseCase().onEach { startDay ->
            _uiState.update { it.copy(payCycleStartDay = startDay) }
            loadMonth(_uiState.value.selectedMonth)
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

    /**
     *  Split the single transaction list into income and spending.
     * Savings count as spending (as they did when they were an expense type)
     * so the remainder is unchanged.
     */
    private suspend fun summariseMonth(start: LocalDate, end: LocalDate) {
        getTransactionsByMonthUseCase(start, end).map { transactions ->
            val income = transactions.filter { it.transactionType == TransactionType.INCOME }
            val spending = transactions.filter { it.transactionType != TransactionType.INCOME }
            val totalIncome = income.sumOf { it.amount }
            val totalExpenses = spending.sumOf { it.amount }
            val byCategory = spending
                .groupBy { it.category.name }
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

    /** Processes a user action from the Dashboard screen. */
    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.PreviousMonth -> loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            DashboardEvent.NextMonth -> loadMonth(_uiState.value.selectedMonth.plusMonths(1))
            DashboardEvent.ShowSettingsSheet ->
                _uiState.update { it.copy(isSettingsSheetOpen = true) }
            DashboardEvent.HideSettingsSheet ->
                _uiState.update { it.copy(isSettingsSheetOpen = false) }
            is DashboardEvent.SetPayCycleStartDay -> viewModelScope.launch {
                setPayCycleStartDayUseCase(event.day)
                _uiState.update { it.copy(isSettingsSheetOpen = false) }
            }
        }
    }
}

/**
 * Immutable snapshot of the Dashboard screen's UI state.
 *
 * [expensesByCategory] maps each category name to the total spent under that category
 * (savings included) for the selected month.
 */
data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val payCycleStartDay: Int = 1,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainder: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val isSettingsSheetOpen: Boolean = false
)

/**
 * User actions on the Dashboard screen.
 */
sealed class DashboardEvent {
    object PreviousMonth : DashboardEvent()
    object NextMonth : DashboardEvent()
    object ShowSettingsSheet : DashboardEvent()
    object HideSettingsSheet : DashboardEvent()
    data class SetPayCycleStartDay(val day: Int) : DashboardEvent()
}
