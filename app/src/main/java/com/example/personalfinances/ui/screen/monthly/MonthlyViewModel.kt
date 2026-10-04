package com.example.personalfinances.ui.screen.monthly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.usecase.category.AddCategoryUseCase
import com.example.personalfinances.domain.usecase.category.GetCategoriesUseCase
import com.example.personalfinances.domain.usecase.merchant.AddMerchantUseCase
import com.example.personalfinances.domain.usecase.merchant.GetMerchantsUseCase
import com.example.personalfinances.domain.usecase.settings.GetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.transaction.AddTransactionUseCase
import com.example.personalfinances.domain.usecase.transaction.DeleteTransactionSeriesUseCase
import com.example.personalfinances.domain.usecase.transaction.GetTransactionSeriesUseCase
import com.example.personalfinances.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.personalfinances.domain.usecase.transaction.GetTransactionsByMonthUseCase
import com.example.personalfinances.domain.usecase.transaction.UpdateTransactionSeriesUseCase
import com.example.personalfinances.domain.usecase.transaction.UpdateTransactionUseCase
import com.example.personalfinances.util.DateUtils
import com.example.personalfinances.util.Recurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 *
 * [undoDelete] holds the most recent delete while its Undo prompt is showing.
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
    val recurringDialog: RecurringDialogState = RecurringDialogState.None,
    val undoDelete: PendingUndo? = null
)

/**
 * A delete the user can still take back: the removed [transactions] and the [message] to show.
 * [token] makes every delete a distinct value, so the screen shows a fresh Undo prompt even when
 * the same transaction is deleted, restored and deleted again.
 */
data class PendingUndo(
    val transactions: List<Transaction>,
    val message: String,
    val token: Long = System.nanoTime()
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

    /** Puts back the transactions removed by the last delete. */
    object UndoLastDelete : CalendarEvent()

    /** The Undo prompt went away without being used. */
    object ClearUndo : CalendarEvent()
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
    private val getTransactionSeriesUseCase: GetTransactionSeriesUseCase,
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

    /** Deletes one transaction and offers to undo it. */
    private suspend fun deleteOne(transaction: Transaction) {
        deleteTransactionUseCase(transaction)
        _uiState.update {
            it.copy(undoDelete = PendingUndo(listOf(transaction), "Deleted ${transaction.category.name}"))
        }
    }

    /**
     * Deletes [transaction] and every later transaction in its series. The whole set is fetched
     * first, since the screen only holds the current month, so Undo can restore all of it.
     */
    private suspend fun deleteSeries(transaction: Transaction) {
        val groupId = transaction.recurringGroupId ?: return deleteOne(transaction)
        val removed = getTransactionSeriesUseCase(groupId, transaction.date)
        deleteTransactionSeriesUseCase(groupId, transaction.date)
        _uiState.update {
            it.copy(undoDelete = PendingUndo(removed, "Deleted ${removed.size} transactions"))
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
                    // Every date is counted from the first, so the original day is kept.
                    val dates = Recurrence.dates(
                        transaction.date, transaction.cadenceUnit, transaction.cadenceValue, event.durationMonths
                    )
                    dates.forEach { date ->
                        // Every copy needs its own id: inserts use REPLACE, so a shared id would
                        // overwrite the previous copy and leave a single row.
                        addTransactionUseCase(
                            transaction.copy(id = UUID.randomUUID().toString(), recurringGroupId = groupId, date = date)
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
                    viewModelScope.launch { deleteOne(event.transaction) }
                }
            }

            is CalendarEvent.ConfirmDelete -> viewModelScope.launch {
                when (event.scope) {
                    RecurringScope.THIS_ONLY -> deleteOne(event.transaction)
                    RecurringScope.THIS_AND_FUTURE -> deleteSeries(event.transaction)
                }
                _uiState.update { it.copy(recurringDialog = RecurringDialogState.None) }
            }

            CalendarEvent.UndoLastDelete -> viewModelScope.launch {
                val pending = _uiState.value.undoDelete
                _uiState.update { it.copy(undoDelete = null) }
                // Same ids as before, so restored transactions keep their identity and series link.
                pending?.transactions?.forEach { addTransactionUseCase(it) }
            }
            CalendarEvent.ClearUndo -> _uiState.update { it.copy(undoDelete = null) }

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
