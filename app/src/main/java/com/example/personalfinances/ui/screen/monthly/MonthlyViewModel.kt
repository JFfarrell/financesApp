package com.example.personalfinances.ui.screen.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.usecase.category.AddCategoryUseCase
import com.example.personalfinances.domain.usecase.category.GetCategoriesUseCase
import com.example.personalfinances.domain.usecase.merchant.AddMerchantUseCase
import com.example.personalfinances.domain.usecase.merchant.GetMerchantsUseCase
import com.example.personalfinances.domain.usecase.settings.GetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.transaction.AddTransactionUseCase
import com.example.personalfinances.domain.usecase.transaction.DeleteTransactionSeriesUseCase
import com.example.personalfinances.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.personalfinances.domain.usecase.transaction.GetTransactionsByMonthUseCase
import com.example.personalfinances.domain.usecase.transaction.UpdateTransactionSeriesUseCase
import com.example.personalfinances.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.personalfinances.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

enum class RecurringScope { THIS_ONLY, THIS_AND_FUTURE }

/** Tracks whether a recurring-scope dialog is waiting for user input and what action it concerns. */
sealed class RecurringDialogState {
    object None : RecurringDialogState()
    data class PendingDelete(val transaction: Transaction) : RecurringDialogState()
    data class PendingUpdate(val updated: Transaction) : RecurringDialogState()
}

/**
 * Immutable snapshot of the Calendar screen's UI state.
 *
 * [transactions] holds the month's one-off transactions of every type; [recurringTransactions]
 * holds the recurring ones. The screen groups them by [Transaction.transactionType].
 *
 * [transactionSheetTarget] is null when the sheet is in Add mode, or holds the transaction being
 * edited. In Add mode, [sheetDefaultType] is the type the sheet should pre-select.
 *
 * [categories] and [merchants] feed the pickers in the transaction sheet.
 *
 * [recurringDialog] is non-None when a recurring-scope prompt is waiting for user input.
 */
data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val payCycleStartDay: Int = 1,
    val transactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val isTransactionSheetOpen: Boolean = false,
    val transactionSheetTarget: Transaction? = null,
    val sheetDefaultType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val merchants: List<Merchant> = emptyList(),
    val recurringDialog: RecurringDialogState = RecurringDialogState.None
)

/** All actions a user can take on the Calendar screen. */
sealed class CalendarEvent {
    object PreviousMonth : CalendarEvent()
    object NextMonth : CalendarEvent()

    data class AddTransaction(
        val transaction: Transaction,
        val durationMonths: Int = 1
    ) : CalendarEvent()
    data class UpdateTransaction(val transaction: Transaction) : CalendarEvent()
    data class DeleteTransaction(val transaction: Transaction) : CalendarEvent()
    data class ShowAddTransactionSheet(val type: TransactionType) : CalendarEvent()
    data class ShowEditTransactionSheet(val transaction: Transaction) : CalendarEvent()
    object HideTransactionSheet : CalendarEvent()

    /** Saves a category created inline in the sheet. The sheet supplies the id so it can select it. */
    data class AddCategory(val category: Category) : CalendarEvent()
    data class AddMerchant(val merchant: Merchant) : CalendarEvent()

    data class ConfirmDelete(val transaction: Transaction, val scope: RecurringScope) : CalendarEvent()
    data class ConfirmUpdate(val transaction: Transaction, val scope: RecurringScope) : CalendarEvent()
    object DismissRecurringDialog : CalendarEvent()
}

/**
 * Holds all UI state for the Calendar screen and handles user-driven events.
 *
 * A single flow of the month's transactions is split into one-off and recurring lists, so both
 * update together and [CalendarUiState.isLoading] clears once the data is ready.
 *
 * For recurring entries (those with a [Transaction.recurringGroupId]), delete and update
 * operations pause and set [CalendarUiState.recurringDialog] so the UI can ask the user whether
 * to apply the change to just this entry or to this and all future entries in the series.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val updateTransactionSeriesUseCase: UpdateTransactionSeriesUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val deleteTransactionSeriesUseCase: DeleteTransactionSeriesUseCase,
    private val getPayCycleStartDayUseCase: GetPayCycleStartDayUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val getMerchantsUseCase: GetMerchantsUseCase,
    private val addMerchantUseCase: AddMerchantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var monthJob: Job? = null

    init {
        getPayCycleStartDayUseCase().onEach { startDay ->
            _uiState.update { it.copy(payCycleStartDay = startDay) }
            loadMonth(_uiState.value.selectedMonth)
        }.launchIn(viewModelScope)

        getCategoriesUseCase().onEach { categories ->
            _uiState.update { it.copy(categories = categories) }
        }.launchIn(viewModelScope)

        getMerchantsUseCase().onEach { merchants ->
            _uiState.update { it.copy(merchants = merchants) }
        }.launchIn(viewModelScope)
    }

    private fun loadMonth(month: YearMonth) {
        val startDay = _uiState.value.payCycleStartDay
        monthJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedMonth = month) }
        monthJob = viewModelScope.launch {
            val (start, end) = DateUtils.monthDateRange(month, startDay)
            getTransactionsByMonthUseCase(start, end).collect { transactions ->
                _uiState.update {
                    it.copy(
                        transactions = transactions.filter { t -> !t.isRecurring },
                        recurringTransactions = transactions.filter { t -> t.isRecurring },
                        isLoading = false
                    )
                }
            }
        }
    }

    /** Returns [base] moved forward by [steps] cadence intervals of [unit], each [value] long. */
    private fun advance(base: LocalDate, unit: CadenceUnit, value: Int, steps: Int): LocalDate {
        val amount = (value * steps).toLong()
        return when (unit) {
            CadenceUnit.DAYS -> base.plusDays(amount)
            CadenceUnit.WEEKS -> base.plusWeeks(amount)
            CadenceUnit.MONTHS -> base.plusMonths(amount)
            CadenceUnit.YEARS -> base.plusYears(amount)
        }
    }

    /** Processes a user action from the Calendar screen. */
    fun onEvent(event: CalendarEvent) {
        when (event) {
            CalendarEvent.PreviousMonth ->
                loadMonth(_uiState.value.selectedMonth.minusMonths(1))
            CalendarEvent.NextMonth ->
                loadMonth(_uiState.value.selectedMonth.plusMonths(1))

            is CalendarEvent.AddTransaction -> viewModelScope.launch {
                val transaction = event.transaction
                if (transaction.isRecurring && event.durationMonths > 1) {
                    val groupId = UUID.randomUUID().toString()
                    val cadence = transaction.cadenceValue.coerceAtLeast(1)
                    repeat(event.durationMonths) { i ->
                        // Every copy needs its own id: inserts use REPLACE, so a shared id would
                        // overwrite the previous copy and leave a single row.
                        addTransactionUseCase(
                            transaction.copy(
                                id = UUID.randomUUID().toString(),
                                recurringGroupId = groupId,
                                date = advance(transaction.date, transaction.cadenceUnit, cadence, i)
                            )
                        )
                    }
                } else {
                    addTransactionUseCase(transaction)
                }
                _uiState.update { it.copy(isTransactionSheetOpen = false, transactionSheetTarget = null) }
            }

            is CalendarEvent.UpdateTransaction -> {
                if (event.transaction.recurringGroupId != null) {
                    _uiState.update {
                        it.copy(
                            isTransactionSheetOpen = false,
                            transactionSheetTarget = null,
                            recurringDialog = RecurringDialogState.PendingUpdate(event.transaction)
                        )
                    }
                } else {
                    viewModelScope.launch { updateTransactionUseCase(event.transaction) }
                    _uiState.update { it.copy(isTransactionSheetOpen = false, transactionSheetTarget = null) }
                }
            }

            is CalendarEvent.DeleteTransaction -> {
                if (event.transaction.recurringGroupId != null) {
                    _uiState.update {
                        it.copy(recurringDialog = RecurringDialogState.PendingDelete(event.transaction))
                    }
                } else {
                    viewModelScope.launch { deleteTransactionUseCase(event.transaction) }
                }
            }

            is CalendarEvent.ConfirmDelete -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> deleteTransactionUseCase(event.transaction)
                    RecurringScope.THIS_AND_FUTURE -> deleteTransactionSeriesUseCase(
                        event.transaction.recurringGroupId!!, event.transaction.date
                    )
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            is CalendarEvent.ConfirmUpdate -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> updateTransactionUseCase(event.transaction)
                    RecurringScope.THIS_AND_FUTURE -> updateTransactionSeriesUseCase(
                        event.transaction.date, event.transaction
                    )
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            is CalendarEvent.ShowAddTransactionSheet ->
                _uiState.update {
                    it.copy(
                        isTransactionSheetOpen = true,
                        transactionSheetTarget = null,
                        sheetDefaultType = event.type
                    )
                }
            is CalendarEvent.ShowEditTransactionSheet ->
                _uiState.update {
                    it.copy(isTransactionSheetOpen = true, transactionSheetTarget = event.transaction)
                }
            CalendarEvent.HideTransactionSheet ->
                _uiState.update { it.copy(isTransactionSheetOpen = false, transactionSheetTarget = null) }

            is CalendarEvent.AddCategory ->
                viewModelScope.launch { addCategoryUseCase(event.category) }
            is CalendarEvent.AddMerchant ->
                viewModelScope.launch { addMerchantUseCase(event.merchant) }

            CalendarEvent.DismissRecurringDialog ->
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
        }
    }
}
