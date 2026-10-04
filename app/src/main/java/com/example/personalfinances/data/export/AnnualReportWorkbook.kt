package com.example.personalfinances.data.export

import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.report.AnnualReport
import com.example.personalfinances.domain.report.ReportRow
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Lays an [AnnualReport] out in the shape of the user's budget spreadsheet ("Annual budget
 * tracker"), sheet for sheet, with a cleaner modern look:
 *  - **Setup**: short instructions and the starting balance (named `StartingBalance`), which is the
 *    app's savings at the start of the year, plus the year, currency and pay-cycle day.
 *  - **Expenses**: recurring expenses only: a column per month and a row per category, under
 *    total, Savings and Leftover rows, with totals and averages.
 *  - **Income**: the same grid for income.
 *  - **Summary**: end-of-month income, expenses, savings, running savings balance and leftover,
 *    then each category by month, and a chart of average spend per category.
 *  - **Spendings**: the one-off expenses (miscellaneous, not part of a repeating series), listed as
 *    Item and Amount under their month.
 *
 * Positions follow the original: categories in column C, January to December in D to O, Total and
 * Average in P and Q, and January's Spendings in J and K. Totals, averages and the summary are real
 * formulas, so the sheet stays live if edited; each is also written with its calculated value for
 * viewers that do not calculate.
 *
 * The app has no category groups, so Expenses has one group, "Recurring", where the original has
 * Fixed Costs, Household Costs and so on. Leftover and the Summary count both recurring and
 * one-off expenses.
 */
object AnnualReportWorkbook {

    fun build(report: AnnualReport, locale: Locale = Locale.getDefault()): XlsxWorkbook {
        val symbol = ReportFormats.symbolFor(report.currencyCode, locale)
        val look = Look(
            money = ReportFormats.moneyFormat(symbol, report.fractionDigits),
            whole = ReportFormats.moneyFormat(symbol, 0)
        )
        val workbook = XlsxWorkbook(look.sheet.toXml())
        workbook.definedNames["StartingBalance"] = "Setup!\$C\$13"

        val figures = Figures(report, locale)
        addSetup(workbook, report, look)
        addExpenses(workbook, figures, look)
        addIncome(workbook, figures, look)
        addSummary(workbook, report, figures, look, symbol)
        addSpendings(workbook, report, figures, look, locale)
        return workbook
    }

    /** The report's numbers, worked out once for the sheets that share them. */
    private class Figures(report: AnnualReport, locale: Locale) {
        val monthLabels = (1..12).map { Month.of(it).getDisplayName(TextStyle.SHORT, locale) }
        val incomeRows: List<ReportRow> = report.rowsByType[TransactionType.INCOME].orEmpty()
        /** Recurring expenses, which are what the Expenses sheet lists. */
        val expenseRows: List<ReportRow> = report.recurringExpenseRows
        val income = report.monthlyTotals(TransactionType.INCOME)
        val recurring = List(12) { m -> expenseRows.sumOf { it.monthly[m] } }
        val onceOff = report.onceOffExpenseTotals

        /** All expenses, recurring and one-off. */
        val expenses = List(12) { recurring[it] + onceOff[it] }
        val savings = report.monthlyTotals(TransactionType.SAVING)

        /** What is left each month after expenses and savings, as on the Home screen. */
        val leftover = List(12) { income[it] - expenses[it] - savings[it] }
    }

    /**
     * Every cell look the report uses, registered once. Navy is the header colour, indigo marks
     * totals, and grey hairlines separate rows.
     */
    private class Look(money: String, whole: String) {
        val sheet = XlsxStyleSheet()

        private val navy = "FF1F2A44"
        private val indigo = "FF4F46E5"
        private val tint = "FFEEF2FF"
        private val band = "FFF3F4F6"
        private val ink = "FF374151"
        private val muted = "FF6B7280"
        private val white = "FFFFFFFF"
        private val hairline = XlsxBorder(bottom = "FFE5E7EB")
        private val topAndBottom = XlsxBorder(top = "FFC7D2FE", bottom = "FFC7D2FE")

        // Titles and text
        val title = sheet.style(XlsxFont(22.0, bold = true, color = navy), align = XlsxAlign.LEFT)
        val heading = sheet.style(XlsxFont(14.0, bold = true, color = navy), align = XlsxAlign.LEFT)
        val body = sheet.style(XlsxFont(10.0, color = ink), align = XlsxAlign.LEFT)
        val bodyWrap = sheet.style(XlsxFont(10.0, color = ink), align = XlsxAlign.LEFT, wrap = true)
        val muted9 = sheet.style(XlsxFont(9.0, italic = true, color = muted), align = XlsxAlign.LEFT)
        val stepNumber = sheet.style(XlsxFont(12.0, bold = true, color = indigo), align = XlsxAlign.RIGHT)
        val setupLabel = sheet.style(XlsxFont(11.0, bold = true, color = ink), align = XlsxAlign.LEFT)
        val setupValue = sheet.style(XlsxFont(11.0, color = ink), align = XlsxAlign.LEFT)
        val input = sheet.style(
            XlsxFont(11.0, bold = true, color = navy), fill = "FFFFF6D5", border = topAndBottom,
            numberFormat = money, align = XlsxAlign.LEFT
        )

        // Header bands
        val strip = sheet.style(fill = navy)
        val headTitle = sheet.style(XlsxFont(14.0, bold = true, color = white), fill = navy, align = XlsxAlign.LEFT, indent = 1)
        val headMonth = sheet.style(XlsxFont(10.0, bold = true, color = white), fill = navy, align = XlsxAlign.RIGHT)
        val headCenter = sheet.style(XlsxFont(10.0, bold = true, color = white), fill = navy, align = XlsxAlign.CENTER)
        val headBlank = strip
        val subHead = sheet.style(XlsxFont(9.0, bold = true, color = muted), fill = band, align = XlsxAlign.LEFT, indent = 1)
        val subHeadRight = sheet.style(XlsxFont(9.0, bold = true, color = muted), fill = band, align = XlsxAlign.RIGHT)

        // Plain rows
        val name = sheet.style(XlsxFont(10.0, color = ink), border = hairline, align = XlsxAlign.LEFT, indent = 1)
        val nameBold = sheet.style(XlsxFont(10.0, bold = true, color = ink), border = hairline, align = XlsxAlign.LEFT, indent = 1)
        val cell = sheet.style(XlsxFont(10.0, color = ink), border = hairline, numberFormat = money, align = XlsxAlign.RIGHT)
        val cellWhole = sheet.style(XlsxFont(10.0, color = ink), border = hairline, numberFormat = whole, align = XlsxAlign.RIGHT)
        val cellBold = sheet.style(XlsxFont(10.0, bold = true, color = ink), border = hairline, numberFormat = money, align = XlsxAlign.RIGHT)
        val sideCell = sheet.style(XlsxFont(10.0, italic = true, color = muted), fill = band, border = hairline, numberFormat = money, align = XlsxAlign.RIGHT)
        val sideCellWhole = sheet.style(XlsxFont(10.0, italic = true, color = muted), fill = band, border = hairline, numberFormat = whole, align = XlsxAlign.RIGHT)
        val itemText = sheet.style(XlsxFont(10.0, color = ink), border = hairline, align = XlsxAlign.LEFT, indent = 1)

        // Group totals (tinted) and overall totals (indigo)
        val groupName = sheet.style(XlsxFont(10.0, bold = true, color = navy), fill = tint, border = topAndBottom, align = XlsxAlign.LEFT, indent = 1)
        val groupCell = sheet.style(XlsxFont(10.0, bold = true, color = navy), fill = tint, border = topAndBottom, numberFormat = money, align = XlsxAlign.RIGHT)
        val groupCellWhole = sheet.style(XlsxFont(10.0, bold = true, color = navy), fill = tint, border = topAndBottom, numberFormat = whole, align = XlsxAlign.RIGHT)
        val groupEdge = sheet.style(fill = tint, border = topAndBottom)
        val overallName = sheet.style(XlsxFont(10.0, bold = true, color = white), fill = indigo, align = XlsxAlign.LEFT, indent = 1)
        val overallCell = sheet.style(XlsxFont(10.0, bold = true, color = white), fill = indigo, numberFormat = money, align = XlsxAlign.RIGHT)
        val overallEdge = sheet.style(fill = indigo)
    }

    // --- Setup ---------------------------------------------------------------------------------

    private fun addSetup(workbook: XlsxWorkbook, report: AnnualReport, look: Look) {
        val sheet = workbook.addSheet("Setup")
        sheet.showGridLines = false
        sheet.columns += listOf(
            XlsxColumn(1, 1, 5.0), XlsxColumn(2, 2, 16.0), XlsxColumn(3, 3, 26.0),
            XlsxColumn(4, 5, 10.0), XlsxColumn(6, 7, 6.0)
        )
        sheet.merges += listOf("B2:C2", "B3:F3", "B7:F7", "B8:F8", "B9:F9")

        sheet.addRow(List(7) { XlsxCell.Blank(look.strip) }, height = 6.0)
        sheet.addRow(listOf(null, XlsxCell.Text("Annual budget tracker", look.title)), height = 42.0)
        sheet.addRow(
            listOf(null, XlsxCell.Text("Plan and track your monthly spending for the entire year.", look.muted9)),
            height = 24.0
        )
        sheet.addRow(emptyList(), height = 10.0)
        sheet.addRow(emptyList(), height = 10.0)
        sheet.addRow(listOf(null, XlsxCell.Text("How to use this sheet", look.heading)), height = 28.0)

        val steps = listOf(
            "1." to "The starting balance in Row 13 is your savings at the start of the year: the starting amount " +
                "from the app plus anything saved in earlier years. You can change it here.",
            "2." to "The 'Expenses' tab lists your recurring expenses and 'Income' your income, each category by month with totals and averages.",
            "3." to "'Spendings' lists your one-off expenses. The 'Summary' tab pulls everything together with a chart. " +
                "Edit any amount and the totals update."
        )
        steps.forEach { (number, step) ->
            sheet.addRow(
                listOf(XlsxCell.Text(number, look.stepNumber), XlsxCell.Text(step, look.bodyWrap)),
                height = if (step.length > 110) 42.0 else 24.0
            )
        }
        sheet.addRow(emptyList(), height = 14.0)
        sheet.addRow(emptyList(), height = 10.0)
        sheet.addRow(listOf(null, XlsxCell.Text("Configure", look.heading)), height = 28.0)

        // Row 13 must stay the starting balance: the instructions and the StartingBalance name point here.
        sheet.addRow(
            listOf(null, XlsxCell.Text("Starting balance:", look.setupLabel), XlsxCell.Number(report.startingBalance, look.input)),
            height = 22.0
        )

        // What this export was made with, so a file can be told apart from other years.
        val cycle = if (report.payCycleStartDay <= 1) "Calendar months" else "Starts on day ${report.payCycleStartDay}"
        listOf("Year:" to report.year.toString(), "Currency:" to report.currencyCode, "Month:" to cycle)
            .forEach { (label, value) ->
                sheet.addRow(
                    listOf(null, XlsxCell.Text(label, look.setupLabel), XlsxCell.Text(value, look.setupValue)),
                    height = 20.0
                )
            }
    }

    // --- Shared pieces of the month grids -------------------------------------------------------

    private fun monthColumn(month: Int) = columnName(3 + month) // January is column D

    private fun averageOfPositives(values: List<Double>): Double =
        values.filter { it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }

    private fun totalCell(row: Int, values: List<Double>, style: Int) =
        XlsxCell.Formula("SUM(D$row:O$row)", values.sum(), style)

    /** The average of the months that have something in them, as the original's summary does. */
    private fun averageCell(row: Int, values: List<Double>, style: Int) =
        XlsxCell.Formula("IFERROR(AVERAGEIF(D$row:O$row,\">0\"),0)", averageOfPositives(values), style)

    /** Twelve amount cells; a month with nothing is left empty, keeping the grid quiet. */
    private fun amounts(values: List<Double>, style: Int): List<XlsxCell> =
        values.map { if (it == 0.0) XlsxCell.Blank(style) else XlsxCell.Number(it, style) }

    private fun blanks(count: Int, style: Int) = List(count) { XlsxCell.Blank(style) }

    private fun monthHeaders(labels: List<String>, style: Int) = labels.map { XlsxCell.Text(it, style) }

    /** Column layout shared by Expenses and Income: narrow margins, names, twelve collapsible months, totals. */
    private fun gridColumns() = listOf(
        XlsxColumn(1, 1, 2.0), XlsxColumn(2, 2, 2.0), XlsxColumn(3, 3, 28.0),
        XlsxColumn(4, 15, 11.0, outlineLevel = 1),
        XlsxColumn(16, 17, 13.0)
    )

    /** One row of a month grid: two margin cells, the label, twelve months, then total and average. */
    private fun gridRow(
        sheet: XlsxSheet, edge: Int, label: XlsxCell, months: List<XlsxCell>,
        total: XlsxCell, average: XlsxCell, height: Double, outlineLevel: Int = 0
    ) = sheet.addRow(
        blanks(2, edge) + label + months + total + average, height = height, outlineLevel = outlineLevel
    )

    // --- Expenses ------------------------------------------------------------------------------

    private fun addExpenses(workbook: XlsxWorkbook, figures: Figures, look: Look) {
        val sheet = workbook.addSheet("Expenses")
        sheet.frozenRows = 2
        sheet.columns += gridColumns()

        sheet.addRow(emptyList(), height = 6.0)
        sheet.addRow(
            blanks(2, look.headBlank) + XlsxCell.Text("Recurring expenses", look.headTitle) +
                monthHeaders(figures.monthLabels, look.headMonth) +
                listOf(XlsxCell.Text("Total", look.headMonth), XlsxCell.Text("Average", look.headMonth)),
            height = 34.0
        )

        val groupRow = 6
        val firstItem = groupRow + 1
        val lastItem = groupRow + figures.expenseRows.size

        // Row 3: everything spent.
        gridRow(
            sheet, look.overallEdge, XlsxCell.Text("Recurring total", look.overallName),
            figures.recurring.mapIndexed { m, v -> XlsxCell.Formula("${monthColumn(m)}$groupRow", v, look.overallCell) },
            totalCell(3, figures.recurring, look.overallCell), averageCell(3, figures.recurring, look.overallCell), 26.0
        )
        // Row 4: what was put aside.
        gridRow(
            sheet, 0, XlsxCell.Text("Savings", look.nameBold), figures.savings.map { XlsxCell.Number(it, look.cell) },
            totalCell(4, figures.savings, look.sideCell), averageCell(4, figures.savings, look.sideCell), 22.0
        )
        // Row 5: income minus recurring and one-off expenses minus savings.
        gridRow(
            sheet, 0, XlsxCell.Text("Leftover (after savings)", look.nameBold),
            figures.leftover.mapIndexed { m, v ->
                val col = monthColumn(m)
                XlsxCell.Formula("Income!${col}3-${col}3-${onceOffTotalRef(m)}-${col}4", v, look.cell)
            },
            totalCell(5, figures.leftover, look.sideCell), averageCell(5, figures.leftover, look.sideCell), 22.0
        )
        sheet.negativeHighlights += "D5:Q5"

        // Row 6: the one group, then a row per category.
        gridRow(
            sheet, look.groupEdge, XlsxCell.Text("Recurring", look.groupName),
            figures.recurring.mapIndexed { m, v ->
                if (figures.expenseRows.isEmpty()) XlsxCell.Number(0.0, look.groupCell)
                else XlsxCell.Formula("SUM(${monthColumn(m)}$firstItem:${monthColumn(m)}$lastItem)", v, look.groupCell)
            },
            totalCell(groupRow, figures.recurring, look.groupCell), averageCell(groupRow, figures.recurring, look.groupCell), 24.0
        )
        figures.expenseRows.forEachIndexed { i, row ->
            val n = firstItem + i
            gridRow(
                sheet, 0, XlsxCell.Text(row.category, look.name), amounts(row.monthly, look.cell),
                totalCell(n, row.monthly, look.sideCell), averageCell(n, row.monthly, look.sideCell), 20.0, outlineLevel = 1
            )
        }
    }

    // --- Income --------------------------------------------------------------------------------

    private fun addIncome(workbook: XlsxWorkbook, figures: Figures, look: Look) {
        val sheet = workbook.addSheet("Income")
        sheet.frozenRows = 2
        sheet.columns += gridColumns()

        sheet.addRow(emptyList(), height = 6.0)
        sheet.addRow(
            blanks(2, look.headBlank) + XlsxCell.Text("Income", look.headTitle) +
                monthHeaders(figures.monthLabels, look.headMonth) +
                listOf(XlsxCell.Text("Total", look.headMonth), XlsxCell.Text("Average", look.headMonth)),
            height = 34.0
        )

        val firstItem = 4
        val lastItem = 3 + figures.incomeRows.size
        gridRow(
            sheet, look.overallEdge, XlsxCell.Text("Income", look.overallName),
            figures.income.mapIndexed { m, v ->
                if (figures.incomeRows.isEmpty()) XlsxCell.Number(0.0, look.overallCell)
                else XlsxCell.Formula("SUM(${monthColumn(m)}$firstItem:${monthColumn(m)}$lastItem)", v, look.overallCell)
            },
            totalCell(3, figures.income, look.overallCell), averageCell(3, figures.income, look.overallCell), 26.0
        )
        figures.incomeRows.forEachIndexed { i, row ->
            val n = firstItem + i
            gridRow(
                sheet, 0, XlsxCell.Text(row.category, look.name), amounts(row.monthly, look.cell),
                totalCell(n, row.monthly, look.sideCell), averageCell(n, row.monthly, look.sideCell), 20.0, outlineLevel = 1
            )
        }
    }

    // --- Summary -------------------------------------------------------------------------------

    private fun addSummary(workbook: XlsxWorkbook, report: AnnualReport, figures: Figures, look: Look, symbol: String) {
        val sheet = workbook.addSheet("Summary")
        sheet.showGridLines = false
        sheet.columns += listOf(
            XlsxColumn(1, 1, 2.0), XlsxColumn(2, 2, 2.0, hidden = true), XlsxColumn(3, 3, 28.0),
            XlsxColumn(4, 15, 11.0), XlsxColumn(16, 16, 13.0)
        )
        sheet.negativeHighlights += "D10:O12"

        val income = figures.incomeRows.size
        val categories = figures.expenseRows.size
        // Where the lists further down start, needed now by the totals at the top.
        val incomeTotalRow = 16
        val overallRow = 20 + income
        val firstCategoryRow = 22 + income

        fun text(value: String, style: Int) = XlsxCell.Text(value, style)
        fun label(value: String, style: Int) = listOf<XlsxCell?>(null, null, text(value, style))

        // Rows 1 to 6: the title and what the sheet is.
        sheet.addRow(emptyList(), height = 6.0)
        sheet.addRow(label("Summary", look.title), height = 36.0)
        sheet.addRow(
            label("This sheet summarises the Expenses and Income tabs. It is all formulas, so it updates by itself.", look.body),
            height = 18.0
        )
        sheet.addRow(
            label("Set your starting balance on the Setup tab; it is the savings you had at the start of the year.", look.muted9),
            height = 16.0
        )
        sheet.addRow(emptyList(), height = 8.0)
        sheet.addRow(label("Summary - End Of Month", look.heading), height = 28.0)

        // Row 7: the month headings.
        sheet.addRow(
            blanks(3, look.headBlank) + monthHeaders(figures.monthLabels, look.headMonth) + text("Average", look.headMonth),
            height = 24.0
        )

        // Rows 8 to 12: the end-of-month figures.
        var running = report.startingBalance
        val endingBalance = figures.savings.map { running += it; running }

        fun summaryRow(
            name: String, nameStyle: Int, values: List<Double>, cellStyle: Int, sideStyle: Int,
            formula: (Int) -> String
        ): Int = sheet.addRow(
            label(name, nameStyle) + values.mapIndexed { m, v -> XlsxCell.Formula(formula(m), v, cellStyle) } +
                averageCell(sheet.rows.size + 1, values, sideStyle),
            height = 22.0
        )

        summaryRow("Income", look.nameBold, figures.income, look.cell, look.sideCell) { "${monthColumn(it)}$incomeTotalRow" }
        summaryRow("Expenses", look.nameBold, figures.expenses, look.cell, look.sideCell) { "${monthColumn(it)}$overallRow" }
        summaryRow("Saved this month", look.nameBold, figures.savings, look.cell, look.sideCell) { "Expenses!${monthColumn(it)}4" }
        summaryRow("Savings balance", look.groupName, endingBalance, look.groupCell, look.groupCell) {
            if (it == 0) "StartingBalance+D10" else "${monthColumn(it - 1)}11+${monthColumn(it)}10"
        }
        summaryRow("Leftover", look.nameBold, figures.leftover, look.cell, look.sideCell) { "Expenses!${monthColumn(it)}5" }
        sheet.addRow(emptyList(), height = 18.0)

        // Income, by category.
        sheet.addRow(label("Income", look.heading), height = 28.0)
        sheet.addRow(sectionHeader(figures, look), height = 24.0)
        check(sheet.rows.size + 1 == incomeTotalRow) { "Summary layout moved: income total row" }
        sheet.addRow(
            detailRow("Income", look.groupName, look.groupCell, look.groupCell, figures.income, sheet.rows.size + 1) { "Income!${monthColumn(it)}3" },
            height = 22.0
        )
        figures.incomeRows.forEachIndexed { i, row ->
            sheet.addRow(
                detailRow(row.category, look.name, look.cell, look.sideCell, row.monthly, sheet.rows.size + 1, hideZero = true) { "Income!${monthColumn(it)}${4 + i}" },
                height = 20.0
            )
        }
        sheet.addRow(emptyList(), height = 18.0)

        // Expenses, by category.
        sheet.addRow(label("Expenses", look.heading), height = 28.0)
        sheet.addRow(sectionHeader(figures, look), height = 24.0)
        check(sheet.rows.size + 1 == overallRow) { "Summary layout moved: overall row" }
        sheet.addRow(
            detailRow("Overall", look.overallName, look.overallCell, look.overallCell, figures.expenses, sheet.rows.size + 1) { "Expenses!${monthColumn(it)}3+${onceOffTotalRef(it)}" },
            height = 22.0
        )
        sheet.addRow(
            detailRow("Leftover (after savings)", look.nameBold, look.cell, look.sideCell, figures.leftover, sheet.rows.size + 1) { "Expenses!${monthColumn(it)}5" },
            height = 22.0
        )
        check(sheet.rows.size + 1 == firstCategoryRow) { "Summary layout moved: first category row" }
        figures.expenseRows.forEachIndexed { i, row ->
            sheet.addRow(
                detailRow(row.category, look.name, look.cell, look.sideCell, row.monthly, sheet.rows.size + 1, hideZero = true) { "Expenses!${monthColumn(it)}${7 + i}" },
                height = 20.0
            )
        }
        // The one-off expenses, which have no categories of their own, as one line.
        sheet.addRow(
            detailRow("Once-off spending", look.name, look.cell, look.sideCell, figures.onceOff, sheet.rows.size + 1, hideZero = true) { onceOffTotalRef(it) },
            height = 20.0
        )
        val closingRow = sheet.addRow(emptyList(), height = 12.0)

        if (categories > 0 || figures.onceOff.any { it > 0 }) {
            val last = firstCategoryRow + categories
            sheet.chart = XlsxChart(
                title = "Average $symbol spent per category",
                categories = "Summary!\$C\$$firstCategoryRow:\$C\$$last",
                values = "Summary!\$P\$$firstCategoryRow:\$P\$$last",
                anchorRow = closingRow
            )
        }
    }

    /** The month headings that open the Income and Expenses lists on the Summary. */
    private fun sectionHeader(figures: Figures, look: Look): List<XlsxCell> =
        blanks(3, look.headBlank) + monthHeaders(figures.monthLabels, look.headMonth) + XlsxCell.Text("Average", look.headMonth)

    /**
     * One line of a Summary list: a label, twelve months each pulled from another sheet with
     * [source], and their average. With [hideZero], months with nothing show as empty cells.
     */
    private fun detailRow(
        label: String, labelStyle: Int, monthStyle: Int, averageStyle: Int,
        values: List<Double>, row: Int, hideZero: Boolean = false, source: (Int) -> String
    ): List<XlsxCell?> =
        listOf<XlsxCell?>(null, null, XlsxCell.Text(label, labelStyle)) +
            values.mapIndexed { m, v ->
                val ref = source(m)
                if (hideZero) XlsxCell.Formula("IF($ref=0,\"\",$ref)", v, monthStyle, blankWhenZero = true)
                else XlsxCell.Formula(ref, v, monthStyle)
            } +
            (if (hideZero) {
                XlsxCell.Formula(
                    "IFERROR(AVERAGEIF(D$row:O$row,\">0\"),\"\")", averageOfPositives(values), averageStyle, blankWhenZero = true
                )
            } else {
                averageCell(row, values, averageStyle)
            })

    // --- Spendings -----------------------------------------------------------------------------

    /** First column (zero-based) of the Item/Amount pair for [month]: January is J and K. */
    private fun itemColumn(month: Int) = 9 + 2 * month

    /** The Spendings cell holding [month]'s one-off total (row 4, in the first cell of the merged pair). */
    private fun onceOffTotalRef(month: Int) = "Spendings!${columnName(itemColumn(month))}4"

    private fun addSpendings(workbook: XlsxWorkbook, report: AnnualReport, figures: Figures, look: Look, locale: Locale) {
        val sheet = workbook.addSheet("Spendings")
        val dayFormat = DateTimeFormatter.ofPattern("d MMM", locale)

        // Each one-off expense as "15 Mar - Merchant (notes)", grouped under the month it counts towards.
        val byMonth = (1..12).map { month ->
            report.transactions
                .filter { it.type == TransactionType.EXPENSE && !it.isRecurring && it.period.monthValue == month }
                .map { t ->
                    val name = t.merchant ?: t.category
                    val note = t.notes?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    "${t.date.format(dayFormat)} - $name$note" to t.amount
                }
        }
        val listEnd = maxOf(200, 5 + (byMonth.maxOfOrNull { it.size } ?: 0))

        // Columns C to I are kept, hidden and empty, so that January starts at J as in the original.
        sheet.columns += listOf(XlsxColumn(1, 1, 2.0), XlsxColumn(2, 2, 18.0), XlsxColumn(3, 9, 12.63, hidden = true)) +
            (0 until 12).flatMap { m ->
                listOf(XlsxColumn(itemColumn(m) + 1, itemColumn(m) + 1, 30.0), XlsxColumn(itemColumn(m) + 2, itemColumn(m) + 2, 12.0))
            } + XlsxColumn(34, 35, 13.0)

        val width = 35
        fun blankRow() = MutableList<XlsxCell?>(width) { null }

        val titles = blankRow().also { cells ->
            cells[0] = XlsxCell.Blank(look.headBlank)
            cells[1] = XlsxCell.Text("Spendings", look.headTitle)
            (2 until 9).forEach { cells[it] = XlsxCell.Blank(look.headBlank) }
            (0 until 12).forEach { m ->
                cells[itemColumn(m)] = XlsxCell.Text(figures.monthLabels[m], look.headCenter)
                cells[itemColumn(m) + 1] = XlsxCell.Blank(look.headCenter)
                sheet.merges += "${columnName(itemColumn(m))}1:${columnName(itemColumn(m) + 1)}1"
            }
            cells[33] = XlsxCell.Text("Total", look.headMonth)
            cells[34] = XlsxCell.Text("Average", look.headMonth)
        }
        sheet.addRow(titles, height = 34.0)
        sheet.addRow(emptyList(), height = 19.5, hidden = true)
        sheet.addRow(emptyList(), height = 19.5, hidden = true)

        val monthTotals = byMonth.map { list -> list.sumOf { it.second } }
        val totals = blankRow().also { cells ->
            cells[0] = XlsxCell.Blank(look.overallEdge)
            cells[1] = XlsxCell.Text("Overall", look.overallName)
            (2 until 9).forEach { cells[it] = XlsxCell.Blank(look.overallEdge) }
            (0 until 12).forEach { m ->
                val amount = columnName(itemColumn(m) + 1)
                cells[itemColumn(m)] = XlsxCell.Formula("SUM(${amount}6:$amount$listEnd)", monthTotals[m], look.overallCell)
                cells[itemColumn(m) + 1] = XlsxCell.Blank(look.overallCell)
                sheet.merges += "${columnName(itemColumn(m))}4:${amount}4"
            }
            val monthCells = (0 until 12).map { "${columnName(itemColumn(it))}4" }
            cells[33] = XlsxCell.Formula("SUM(${monthCells.joinToString(",")})", monthTotals.sum(), look.overallCell)
            cells[34] = XlsxCell.Formula(
                "IFERROR(AH4/MAX(1,${monthCells.joinToString("+") { "($it>0)" }}),0)",
                monthTotals.sum() / maxOf(1, monthTotals.count { it > 0 }), look.overallCell
            )
        }
        sheet.addRow(totals, height = 26.0)

        val headings = blankRow().also { cells ->
            (0 until 12).forEach { m ->
                cells[itemColumn(m)] = XlsxCell.Text("Item", look.subHead)
                cells[itemColumn(m) + 1] = XlsxCell.Text("Amount", look.subHeadRight)
            }
        }
        sheet.addRow(headings, height = 22.0)

        (0 until (byMonth.maxOfOrNull { it.size } ?: 0)).forEach { i ->
            val cells = blankRow()
            byMonth.forEachIndexed { m, list ->
                list.getOrNull(i)?.let { (item, amount) ->
                    cells[itemColumn(m)] = XlsxCell.Text(item, look.itemText)
                    cells[itemColumn(m) + 1] = XlsxCell.Number(amount, look.cell)
                }
            }
            sheet.addRow(cells, height = 20.0)
        }
    }
}
