package com.example.personalfinances.domain.usecase.report

import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.repository.ReportRepository
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import com.example.personalfinances.domain.repository.SettingsRepository
import com.example.personalfinances.domain.repository.TransactionRepository
import com.example.personalfinances.domain.report.buildAnnualReport
import com.example.personalfinances.util.DateUtils
import com.example.personalfinances.util.MoneyFormatter
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

/**
 * Exports a year of transactions to an Excel workbook at [destination].
 *
 * The year runs from the start of January's pay cycle to the end of December's, so it lines up
 * with the months shown in the app. The summarising itself is [buildAnnualReport]; this class
 * only gathers the inputs and hands the result to the file writer.
 *
 * The report's starting balance is the savings the app had at the start of the year: the savings
 * starting amount plus every saving transaction dated before the year.
 */
class ExportAnnualReportUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val settings: SettingsRepository,
    private val savingsGoals: SavingsGoalRepository,
    private val reports: ReportRepository
) {
    suspend operator fun invoke(destination: String, year: Int): OperationResult {
        val startDay = settings.getPayCycleStartDay().first()
        val start = DateUtils.monthDateRange(YearMonth.of(year, 1), startDay).first
        val end = DateUtils.monthDateRange(YearMonth.of(year, 12), startDay).second
        val yearTransactions = transactions.getByDateRange(start, end).first()

        val startingAmount = savingsGoals.getSavingsGoal().first()?.startingAmount ?: 0.0
        val savedBefore = transactions.getTotalByType(TransactionType.SAVING, start.minusDays(1)).first()

        val currency = MoneyFormatter.resolve(settings.getCurrencyCode().first())
        val report = buildAnnualReport(
            year = year,
            payCycleStartDay = startDay,
            currencyCode = currency.currencyCode,
            fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0),
            transactions = yearTransactions,
            startingBalance = startingAmount + savedBefore
        )
        return reports.writeAnnualReport(destination, report)
    }
}
