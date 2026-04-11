package com.example.personalfinances.ui.screen.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Income
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

data class IncomeUiState(
    val incomes: List<Income> = emptyList(),
    val isAddSheetVisible: Boolean = false,
    val isLoading: Boolean = true
)

sealed class IncomeEvent {
    data class AddIncome(
        val amount: Double,
        val source: String,
        val cadenceMonths: Int,
        val startDate: Long
    ) : IncomeEvent()
    data class DeleteIncome(val income: Income) : IncomeEvent()
    object ShowAddSheet : IncomeEvent()
    object HideAddSheet : IncomeEvent()
}

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

    fun onEvent(event: IncomeEvent) {
        when (event) {
            is IncomeEvent.AddIncome -> viewModelScope.launch {
                addIncomeUseCase(
                    Income(
                        amount = event.amount,
                        source = event.source,
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
