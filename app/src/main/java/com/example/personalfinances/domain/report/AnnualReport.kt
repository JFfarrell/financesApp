package com.example.personalfinances.domain.report

import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

/** One category's amounts for the 12 months of a report, January first. */
data class ReportRow(val category: String, val monthly: List<Double>) {
    val total: Double get() = monthly.sum()
}

/** One transaction flattened for the report's transaction list. */
data class ReportTransaction(
    val date: LocalDate,
    val type: TransactionType,
    val category: String,
    val merchant: String?,
    val amount: Double,
    val notes: String?,
    val tags: List<String>,
    val isRecurring: Boolean,
    val period: YearMonth
)

/**
 * A year of transactions summarised for a spreadsheet: for each transaction type, one row per
 * category with an amount for each of the 12 pay-cycle months, plus the underlying transactions.
 * Expenses can also be viewed in two parts: those that repeat ([recurringExpenseRows]) and
 * one-offs ([onceOffExpenseTotals]).
 *
 * [payCycleStartDay] decides which month a date counts towards, exactly as in the app.
 * [startingBalance] is the savings balance at the start of the year: the starting amount set in the
 * app plus everything saved in earlier years. The report's running savings balance builds on it.
 * [currencyCode] and [fractionDigits] only describe how amounts should be labelled and rounded
 * for display; the amounts themselves are the stored numbers.
 */
data class AnnualReport(
    val year: Int,
    val payCycleStartDay: Int,
    val currencyCode: String,
    val fractionDigits: Int,
    val rowsByType: Map<TransactionType, List<ReportRow>>,
    val transactions: List<ReportTransaction>,
    val startingBalance: Double = 0.0
) {
    /** Per-month total for one transaction type, January first. */
    fun monthlyTotals(type: TransactionType): List<Double> =
        List(12) { month -> rowsByType[type].orEmpty().sumOf { it.monthly[month] } }

    private val expenseTransactions: List<ReportTransaction>
        get() = transactions.filter { it.type == TransactionType.EXPENSE }

    /** One row per category for expenses that repeat (part of a recurring series), sorted by name. */
    val recurringExpenseRows: List<ReportRow>
        get() = expenseTransactions.filter { it.isRecurring }
            .groupBy { it.category }
            .map { (category, entries) ->
                val months = MutableList(12) { 0.0 }
                entries.forEach { months[it.period.monthValue - 1] += it.amount }
                ReportRow(category, months)
            }
            .sortedBy { it.category.lowercase() }

    /** Per-month total of one-off expenses (those not part of a recurring series), January first. */
    val onceOffExpenseTotals: List<Double>
        get() = List(12) { month ->
            expenseTransactions.filter { !it.isRecurring && it.period.monthValue == month + 1 }.sumOf { it.amount }
        }
}

/**
 * Builds the report for [year] from [transactions]. Transactions whose pay-cycle month falls
 * outside the year are ignored, so it is safe to pass a slightly wider list. Categories are
 * grouped by name (as on Home), each type's rows are sorted by name, and the transaction list is
 * sorted by date.
 */
fun buildAnnualReport(
    year: Int,
    payCycleStartDay: Int,
    currencyCode: String,
    fractionDigits: Int,
    transactions: List<Transaction>,
    startingBalance: Double = 0.0
): AnnualReport {
    val inYear = transactions
        .map { it to DateUtils.cycleMonthOf(it.date, payCycleStartDay) }
        .filter { (_, period) -> period.year == year }

    val rows = TransactionType.entries.associateWith { type ->
        inYear
            .filter { (transaction, _) -> transaction.transactionType == type }
            .groupBy { (transaction, _) -> transaction.category.name }
            .map { (category, entries) ->
                val months = MutableList(12) { 0.0 }
                entries.forEach { (transaction, period) -> months[period.monthValue - 1] += transaction.amount }
                ReportRow(category, months)
            }
            .sortedBy { it.category.lowercase() }
    }

    val list = inYear
        .map { (t, period) ->
            ReportTransaction(
                date = t.date,
                type = t.transactionType,
                category = t.category.name,
                merchant = t.merchant?.name,
                amount = t.amount,
                notes = t.notes,
                tags = t.tags.sorted(),
                isRecurring = t.isRecurring,
                period = period
            )
        }
        .sortedWith(compareBy({ it.date }, { it.category.lowercase() }))

    return AnnualReport(year, payCycleStartDay, currencyCode, fractionDigits, rows, list, startingBalance)
}
