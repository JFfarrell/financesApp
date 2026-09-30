package com.example.personalfinances.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.usecase.backup.ExportBackupUseCase
import com.example.personalfinances.domain.usecase.backup.GetLastBackupUseCase
import com.example.personalfinances.domain.usecase.backup.ImportBackupUseCase
import com.example.personalfinances.domain.usecase.transaction.GetFirstTransactionDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

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

/**
 * State for backup and restore. [lastBackupAt] is when the last export finished (epoch
 * milliseconds, null if never); [message] is the result line after an export or import
 * ([isError] marks a failure); [pendingImport] holds a chosen file awaiting the user's
 * confirmation; [reminder] is non-null when the user should be nudged to back up, unless they
 * chose Later ([reminderDismissed]) for this session.
 */
data class BackupUiState(
    val lastBackupAt: Long? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val pendingImport: String? = null,
    val reminder: BackupReminder? = null,
    val reminderDismissed: Boolean = false
)

/** User actions for backup and restore. */
sealed class BackupEvent {
    data class Export(val destination: String) : BackupEvent()
    data class RequestImport(val source: String) : BackupEvent()
    object ConfirmImport : BackupEvent()
    object CancelImport : BackupEvent()
    object DismissMessage : BackupEvent()
    object DismissReminder : BackupEvent()
}

/**
 * Runs backup exports and imports and reports the outcome. Used by both Home (for the reminder
 * card) and Settings; each screen gets its own instance, and they share the saved "last backup"
 * time, so completing a backup in one clears the reminder in the other.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    getLastBackupUseCase: GetLastBackupUseCase,
    getFirstTransactionDateUseCase: GetFirstTransactionDateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        combine(getLastBackupUseCase(), getFirstTransactionDateUseCase()) { lastBackupAt, firstDate ->
            lastBackupAt to reminderFor(lastBackupAt = lastBackupAt, hasData = firstDate != null)
        }.onEach { (lastBackupAt, reminder) ->
            _uiState.update { it.copy(lastBackupAt = lastBackupAt, reminder = reminder) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: BackupEvent) {
        when (event) {
            is BackupEvent.Export -> perform(isImport = false) { exportBackupUseCase(event.destination) }
            // Importing can overwrite records, so the user confirms before anything is read.
            is BackupEvent.RequestImport -> _uiState.update { it.copy(pendingImport = event.source) }
            BackupEvent.CancelImport -> _uiState.update { it.copy(pendingImport = null) }
            BackupEvent.ConfirmImport -> {
                val source = _uiState.value.pendingImport
                _uiState.update { it.copy(pendingImport = null) }
                if (source != null) perform(isImport = true) { importBackupUseCase(source) }
            }
            BackupEvent.DismissMessage -> _uiState.update { it.copy(message = null) }
            BackupEvent.DismissReminder -> _uiState.update { it.copy(reminderDismissed = true) }
        }
    }

    /** Runs a backup [action], showing a busy state and then a one-line result message. */
    private fun perform(isImport: Boolean, action: suspend () -> BackupResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val result = action()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = describe(result, isImport),
                    isError = result is BackupResult.Failure
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
