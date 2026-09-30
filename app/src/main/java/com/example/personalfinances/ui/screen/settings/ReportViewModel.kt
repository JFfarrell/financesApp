package com.example.personalfinances.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.usecase.report.ExportAnnualReportUseCase
import com.example.personalfinances.domain.usecase.transaction.GetFirstTransactionDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * State for the Excel export. [years] lists the years the user can export, newest first, from the
 * year of their earliest transaction to this year. [message] is the result line after an export.
 */
data class ReportUiState(
    val years: List<Int> = listOf(LocalDate.now().year),
    val isBusy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

/** User actions for the Excel export. */
sealed class ReportEvent {
    data class ExportYear(val year: Int, val destination: String) : ReportEvent()
    object DismissMessage : ReportEvent()
}

/** Exports a year of transactions to an Excel workbook and reports the outcome. */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val exportAnnualReportUseCase: ExportAnnualReportUseCase,
    getFirstTransactionDateUseCase: GetFirstTransactionDateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        getFirstTransactionDateUseCase().onEach { first ->
            val thisYear = LocalDate.now().year
            val firstYear = minOf(first?.year ?: thisYear, thisYear)
            _uiState.update { it.copy(years = (firstYear..thisYear).toList().reversed()) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ReportEvent) {
        when (event) {
            is ReportEvent.ExportYear -> viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, message = null) }
                val result = exportAnnualReportUseCase(event.destination, event.year)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = when (result) {
                            OperationResult.Success -> "Excel report for ${event.year} saved."
                            is OperationResult.Failure -> result.message
                        },
                        isError = result is OperationResult.Failure
                    )
                }
            }
            ReportEvent.DismissMessage -> _uiState.update { it.copy(message = null) }
        }
    }
}
