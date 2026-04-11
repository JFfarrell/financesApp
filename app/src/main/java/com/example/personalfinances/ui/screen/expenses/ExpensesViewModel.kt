package com.example.personalfinances.ui.screen.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.usecase.category.AddCategoryUseCase
import com.example.personalfinances.domain.usecase.category.GetCategoriesUseCase
import com.example.personalfinances.domain.usecase.expense.AddExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.DeleteExpenseUseCase
import com.example.personalfinances.domain.usecase.expense.GetExpensesUseCase
import com.example.personalfinances.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isAddSheetVisible: Boolean = false,
    val isLoading: Boolean = true
)

sealed class ExpensesEvent {
    data class AddExpense(
        val amount: Double,
        val title: String,
        val description: String,
        val categoryId: Long?,
        val date: Long,
        val isRecurring: Boolean,
        val cadenceMonths: Int
    ) : ExpensesEvent()
    data class DeleteExpense(val expense: Expense) : ExpensesEvent()
    object ShowAddSheet : ExpensesEvent()
    object HideAddSheet : ExpensesEvent()
    data class AddCategory(val name: String) : ExpensesEvent()
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getExpensesUseCase(),
                getCategoriesUseCase()
            ) { expenses, categories ->
                expenses to categories
            }.collect { (expenses, categories) ->
                _uiState.update {
                    it.copy(expenses = expenses, categories = categories, isLoading = false)
                }
            }
        }
    }

    fun onEvent(event: ExpensesEvent) {
        when (event) {
            is ExpensesEvent.AddExpense -> viewModelScope.launch {
                addExpenseUseCase(
                    Expense(
                        amount = event.amount,
                        title = event.title,
                        description = event.description,
                        categoryId = event.categoryId,
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
            is ExpensesEvent.AddCategory -> viewModelScope.launch {
                addCategoryUseCase(Category(name = event.name))
            }
            ExpensesEvent.ShowAddSheet -> _uiState.update { it.copy(isAddSheetVisible = true) }
            ExpensesEvent.HideAddSheet -> _uiState.update { it.copy(isAddSheetVisible = false) }
        }
    }
}
