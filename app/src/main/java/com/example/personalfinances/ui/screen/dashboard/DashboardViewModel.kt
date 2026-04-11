package com.example.personalfinances.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.usecase.category.GetCategoriesUseCase
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

data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainder: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true
)

sealed class DashboardEvent {
    object PreviousMonth : DashboardEvent()
    object NextMonth : DashboardEvent()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getExpensesByMonthUseCase: GetExpensesByMonthUseCase,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
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
                getIncomesUseCase(start, end),
                getCategoriesUseCase()
            ) { expenses, incomes, categories ->
                val categoryMap: Map<Long, Category> = categories.associateBy { it.id }
                val totalExpenses = expenses.sumOf { it.amount }
                val totalIncome = incomes.sumOf { it.amount }
                val byCategory = expenses
                    .groupBy { expense ->
                        expense.categoryId?.let { categoryMap[it]?.name } ?: "Uncategorized"
                    }
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

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.PreviousMonth -> loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            DashboardEvent.NextMonth -> loadMonth(_uiState.value.selectedMonth.plusMonths(1))
        }
    }
}
