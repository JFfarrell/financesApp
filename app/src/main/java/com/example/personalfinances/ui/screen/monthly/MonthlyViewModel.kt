package com.example.personalfinances.ui.screen.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.usecase.expense.AddExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.DeleteExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.GetExpensesByMonthUseCase
import com.example.personalfinances.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class MonthlyUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val expenses: List<Expense> = emptyList(),
    val recurringExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true
)

sealed class MonthlyEvent {
    object PreviousMonth : MonthlyEvent()
    object NextMonth : MonthlyEvent()
    data class DeleteExpense(val expense: Expense) : MonthlyEvent()
}

@HiltViewModel
class MonthlyViewModel @Inject constructor(
    private val getExpensesByMonthUseCase: GetExpensesByMonthUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyUiState())
    val uiState: StateFlow<MonthlyUiState> = _uiState.asStateFlow()

    private var monthJob: Job? = null

    init {
        loadMonth(YearMonth.now())
    }

    private fun loadMonth(month: YearMonth) {
        monthJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedMonth = month) }
        monthJob = viewModelScope.launch {
            val (start, end) = DateUtils.monthBounds(month)
            getExpensesByMonthUseCase(start, end).collect { expenses ->
                _uiState.update {
                    it.copy(
                        expenses = expenses.filter { e -> !e.isRecurring },
                        recurringExpenses = expenses.filter { e -> e.isRecurring },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onEvent(event: MonthlyEvent) {
        when (event) {
            MonthlyEvent.PreviousMonth -> loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            MonthlyEvent.NextMonth -> loadMonth(_uiState.value.selectedMonth.plusMonths(1))
            is MonthlyEvent.DeleteExpense -> viewModelScope.launch {
                deleteExpenseUseCase(event.expense)
            }
        }
    }
}
