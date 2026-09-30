package com.example.personalfinances.domain.report

import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Plain JVM tests for how transactions are summarised; run with `./gradlew :app:testDebugUnitTest`. */
class AnnualReportTest {

    private fun tx(
        category: String,
        type: TransactionType,
        amount: Double,
        date: LocalDate
    ) = Transaction(
        id = "$category-$date-$amount", transactionType = type, amount = amount, date = date,
        cadenceUnit = CadenceUnit.MONTHS, cadenceValue = 0, category = Category(category, category, type),
        merchant = null, isRecurring = false, recurringGroupId = null, notes = null, tags = emptySet()
    )

    private fun report(startDay: Int, vararg transactions: Transaction) =
        buildAnnualReport(2026, startDay, "EUR", 2, transactions.toList())

    @Test
    fun amountsAreAddedPerCategoryAndMonth() {
        val r = report(
            1,
            tx("Groceries", TransactionType.EXPENSE, 10.0, LocalDate.of(2026, 3, 2)),
            tx("Groceries", TransactionType.EXPENSE, 5.5, LocalDate.of(2026, 3, 30)),
            tx("Groceries", TransactionType.EXPENSE, 7.0, LocalDate.of(2026, 4, 1))
        )
        val row = r.rowsByType.getValue(TransactionType.EXPENSE).single()
        assertEquals("Groceries", row.category)
        assertEquals(15.5, row.monthly[2], 0.0)
        assertEquals(7.0, row.monthly[3], 0.0)
        assertEquals(22.5, row.total, 0.0)
    }

    @Test
    fun typesAreKeptSeparate() {
        val r = report(
            1,
            tx("Other", TransactionType.EXPENSE, 10.0, LocalDate.of(2026, 1, 5)),
            tx("Other", TransactionType.INCOME, 99.0, LocalDate.of(2026, 1, 5))
        )
        assertEquals(10.0, r.monthlyTotals(TransactionType.EXPENSE)[0], 0.0)
        assertEquals(99.0, r.monthlyTotals(TransactionType.INCOME)[0], 0.0)
        assertEquals(0.0, r.monthlyTotals(TransactionType.SAVING)[0], 0.0)
    }

    @Test
    fun aPayCycleIsNamedForTheMonthItStartsIn() {
        // With a start day of 25, "August" runs 25 Aug to 24 Sep, so 24 Aug is still July's.
        val r = report(
            25,
            tx("Rent", TransactionType.EXPENSE, 100.0, LocalDate.of(2026, 8, 24)),
            tx("Rent", TransactionType.EXPENSE, 200.0, LocalDate.of(2026, 8, 25))
        )
        val months = r.rowsByType.getValue(TransactionType.EXPENSE).single().monthly
        assertEquals(100.0, months[6], 0.0)   // July
        assertEquals(200.0, months[7], 0.0)   // August
    }

    @Test
    fun earlyJanuaryDatesBelongToThePreviousYearsDecemberCycle() {
        // With a start day of 25, 2026's December cycle runs 25 Dec 2026 to 24 Jan 2027, and the
        // year's first cycle starts on 25 Jan 2026, so 24 Jan 2026 belongs to December 2025.
        val r = report(
            25,
            tx("Rent", TransactionType.EXPENSE, 100.0, LocalDate.of(2026, 1, 24)),  // Dec 2025: left out
            tx("Rent", TransactionType.EXPENSE, 300.0, LocalDate.of(2026, 1, 25)),  // Jan 2026
            tx("Rent", TransactionType.EXPENSE, 50.0, LocalDate.of(2027, 1, 10))    // Dec 2026
        )
        val totals = r.monthlyTotals(TransactionType.EXPENSE)
        assertEquals(300.0, totals[0], 0.0)
        assertEquals(50.0, totals[11], 0.0)
        assertEquals(2, r.transactions.size)
    }

    @Test
    fun categoriesAreSortedByNameAndTransactionsByDate() {
        val r = report(
            1,
            tx("zebra", TransactionType.EXPENSE, 1.0, LocalDate.of(2026, 5, 9)),
            tx("Apple", TransactionType.EXPENSE, 1.0, LocalDate.of(2026, 5, 2))
        )
        assertEquals(listOf("Apple", "zebra"), r.rowsByType.getValue(TransactionType.EXPENSE).map { it.category })
        assertEquals(listOf(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 9)), r.transactions.map { it.date })
    }

    @Test
    fun cycleMonthMatchesTheMonthRangeItInverts() {
        listOf(1, 15, 25, 31).forEach { startDay ->
            var date = LocalDate.of(2026, 1, 1)
            while (date.year == 2026) {
                val month = DateUtils.cycleMonthOf(date, startDay)
                val (start, end) = DateUtils.monthDateRange(month, startDay)
                assertTrue("$date with start day $startDay", date in start..end)
                date = date.plusDays(1)
            }
        }
        assertEquals(YearMonth.of(2026, 7), DateUtils.cycleMonthOf(LocalDate.of(2026, 8, 24), 25))
        assertEquals(YearMonth.of(2026, 8), DateUtils.cycleMonthOf(LocalDate.of(2026, 8, 25), 25))
    }

    @Test
    fun expensesAreSplitIntoRecurringByCategoryAndOnceOffTotals() {
        val r = report(
            1,
            tx("Gym", TransactionType.EXPENSE, 30.0, LocalDate.of(2026, 1, 1)).copy(isRecurring = true),
            tx("Gym", TransactionType.EXPENSE, 30.0, LocalDate.of(2026, 2, 1)).copy(isRecurring = true),
            tx("Shoes", TransactionType.EXPENSE, 80.0, LocalDate.of(2026, 2, 10)),
            tx("Salary", TransactionType.INCOME, 2000.0, LocalDate.of(2026, 2, 1)).copy(isRecurring = true)
        )

        val recurring = r.recurringExpenseRows.single()
        assertEquals("Gym", recurring.category)
        assertEquals(30.0, recurring.monthly[0], 0.0)
        assertEquals(30.0, recurring.monthly[1], 0.0)

        val onceOff = r.onceOffExpenseTotals
        assertEquals(0.0, onceOff[0], 0.0)
        assertEquals(80.0, onceOff[1], 0.0)

        // Together they are all the expenses; recurring income does not count as an expense.
        assertEquals(r.monthlyTotals(TransactionType.EXPENSE)[1], recurring.monthly[1] + onceOff[1], 0.0)
    }
}
