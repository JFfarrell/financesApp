package com.example.personalfinances.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.usecase.backup.ExportBackupUseCase
import com.example.personalfinances.domain.usecase.backup.GetLastBackupUseCase
import com.example.personalfinances.domain.usecase.backup.ImportBackupUseCase
import com.example.personalfinances.domain.usecase.savings.GetSavingsGoalUseCase
import com.example.personalfinances.domain.usecase.settings.GetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.settings.GetThemeModeUseCase
import com.example.personalfinances.domain.usecase.settings.SetPayCycleStartDayUseCase
import com.example.personalfinances.domain.usecase.settings.SetThemeModeUseCase
import com.example.personalfinances.domain.usecase.transaction.GetFirstTransactionDateUseCase
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
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import javax.inject.Inject

/**
 * Holds all UI state for the Home (dashboard) screen.
 *
 * Loads the month's transactions reactively and summarises them into income, spending, savings
 * and a per-category breakdown. When the user navigates between months, the previous month's
 * collection job is cancelled and a new one started, preventing stale data from leaking across
 * navigations. It also tracks the savings goal progress and the user's theme choice, both shown
 * on Home and in its settings sheet.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsByMonthUseCase: GetTransactionsByMonthUseCase,
    private val getPayCycleStartDayUseCase: GetPayCycleStartDayUseCase,
    private val setPayCycleStartDayUseCase: SetPayCycleStartDayUseCase,
    private val getSavingsGoalUseCase: GetSavingsGoalUseCase,
    private val getSavingsTotalUseCase: GetSavingsTotalUseCase,
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val getLastBackupUseCase: GetLastBackupUseCase,
    private val getFirstTransactionDateUseCase: GetFirstTransactionDateUseCase
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

        getThemeModeUseCase().onEach { mode ->
            _uiState.update { it.copy(themeMode = mode) }
        }.launchIn(viewModelScope)

        getLastBackupUseCase().onEach { time ->
            _uiState.update { it.copy(lastBackupAt = time) }
        }.launchIn(viewModelScope)

        // Remind the user to back up once there is something to lose and the last backup is old.
        combine(getLastBackupUseCase(), getFirstTransactionDateUseCase()) { lastBackupAt, firstDate ->
            reminderFor(lastBackupAt = lastBackupAt, hasData = firstDate != null)
        }.onEach { reminder ->
            _uiState.update { it.copy(backupReminder = reminder) }
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
            DashboardEvent.ShowSettingsSheet ->
                _uiState.update { it.copy(isSettingsSheetOpen = true) }
            DashboardEvent.HideSettingsSheet ->
                _uiState.update { it.copy(isSettingsSheetOpen = false) }
            is DashboardEvent.SetPayCycleStartDay -> viewModelScope.launch {
                setPayCycleStartDayUseCase(event.day)
                _uiState.update { it.copy(isSettingsSheetOpen = false) }
            }
            is DashboardEvent.SetThemeMode -> viewModelScope.launch {
                setThemeModeUseCase(event.mode)
            }
            DashboardEvent.DismissBackupReminder -> _uiState.update { it.copy(reminderDismissed = true) }
            DashboardEvent.DismissBackupMessage -> _uiState.update { it.copy(backupMessage = null) }
            is DashboardEvent.ExportBackup -> runBackup(isImport = false) { exportBackupUseCase(event.destination) }
            // Importing can overwrite records, so the user confirms before anything is read.
            is DashboardEvent.RequestImport -> _uiState.update { it.copy(pendingImport = event.source) }
            DashboardEvent.CancelImport -> _uiState.update { it.copy(pendingImport = null) }
            DashboardEvent.ConfirmImport -> {
                val source = _uiState.value.pendingImport
                _uiState.update { it.copy(pendingImport = null) }
                if (source != null) runBackup(isImport = true) { importBackupUseCase(source) }
            }
        }
    }

    /** Runs a backup [action], showing a busy state and then a one-line result message. */
    private fun runBackup(isImport: Boolean, action: suspend () -> BackupResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupBusy = true, backupMessage = null) }
            val result = action()
            _uiState.update {
                it.copy(
                    isBackupBusy = false,
                    backupMessage = describe(result, isImport),
                    backupIsError = result is BackupResult.Failure
                )
            }
        }
    }

    private fun describe(result: BackupResult, isImport: Boolean): String = when (result) {
        is BackupResult.Success -> {
            val counts = "${result.transactions} transactions, ${result.categories} categories, " +
                "${result.merchants} merchants"
            if (isImport) "Imported $counts." else "Backup saved: $counts."
        }
        is BackupResult.Failure -> result.message
    }
}

/** How long ago the last backup was, in whole days, or null if there never was one. */
data class BackupReminder(val daysSince: Int?)

/** Days after which a backup is considered out of date. */
private const val REMINDER_AFTER_DAYS = 14

/**
 * Decides whether to nudge the user to back up: never before they have entered anything, always
 * if they have data but no backup, and otherwise once the last backup is [REMINDER_AFTER_DAYS]
 * days old.
 */
private fun reminderFor(lastBackupAt: Long?, hasData: Boolean): BackupReminder? {
    if (lastBackupAt == null) return if (hasData) BackupReminder(daysSince = null) else null
    val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(lastBackupAt), Instant.now()).toInt()
    return if (days >= REMINDER_AFTER_DAYS) BackupReminder(days) else null
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
 * when no goal has been set. [themeMode] is the user's saved appearance choice.
 *
 * Backup: [lastBackupAt] is when the last export finished (epoch milliseconds, null if never),
 * [backupMessage] is the result line shown after an export or import ([backupIsError] marks a
 * failure), and [pendingImport] holds a chosen file awaiting the user's confirmation.
 * [backupReminder] is non-null when the user should be nudged to back up, unless they chose
 * Later ([reminderDismissed]) for this session.
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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastBackupAt: Long? = null,
    val isBackupBusy: Boolean = false,
    val backupMessage: String? = null,
    val backupIsError: Boolean = false,
    val pendingImport: String? = null,
    val backupReminder: BackupReminder? = null,
    val reminderDismissed: Boolean = false,
    val isLoading: Boolean = true,
    val isSettingsSheetOpen: Boolean = false
)

/**
 * User actions on the Home screen.
 */
sealed class DashboardEvent {
    object PreviousMonth : DashboardEvent()
    object NextMonth : DashboardEvent()
    object ShowSettingsSheet : DashboardEvent()
    object HideSettingsSheet : DashboardEvent()
    data class SetPayCycleStartDay(val day: Int) : DashboardEvent()
    data class SetThemeMode(val mode: ThemeMode) : DashboardEvent()
    data class ExportBackup(val destination: String) : DashboardEvent()
    data class RequestImport(val source: String) : DashboardEvent()
    object ConfirmImport : DashboardEvent()
    object CancelImport : DashboardEvent()
    object DismissBackupReminder : DashboardEvent()
    object DismissBackupMessage : DashboardEvent()
}
