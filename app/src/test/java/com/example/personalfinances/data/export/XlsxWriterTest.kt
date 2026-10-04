package com.example.personalfinances.data.export

import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.report.buildAnnualReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Plain JVM tests for the workbook writer and the annual report, which follows the user's budget template. They cannot open
 * the file in Excel, so they check what can go wrong silently: the zip has the required parts,
 * every part is well-formed XML, special characters are escaped, cells and formulas are addressed
 * correctly, and the report has the template's sheets and only uses styles its style sheet defines.
 * Run with `./gradlew :app:testDebugUnitTest`.
 */
class XlsxWriterTest {

    private val tinyStyles =
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font/></fonts>""" +
            """<fills count="1"><fill/></fills><borders count="1"><border/></borders>""" +
            """<cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="2"><xf/><xf/></cellXfs></styleSheet>"""

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val parts = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                parts[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return parts
    }

    private fun parse(xml: String): Document =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

    private fun assertWellFormed(parts: Map<String, String>) = parts.forEach { (name, xml) ->
        try {
            parse(xml)
        } catch (e: Exception) {
            throw AssertionError("$name is not well-formed XML: ${e.message}")
        }
    }

    private fun simpleWorkbook(): XlsxWorkbook {
        val workbook = XlsxWorkbook(tinyStyles)
        val sheet = workbook.addSheet("Data")
        sheet.addRow(listOf(XlsxCell.Text("Name", 1), XlsxCell.Text("Amount", 1)))
        sheet.addRow(listOf(XlsxCell.Text("Fish & \"chips\" <fresh>"), XlsxCell.Number(12.5, 1)))
        sheet.addRow(listOf(XlsxCell.Text("Total"), XlsxCell.Formula("SUM(B2:B2)", 12.5, 1)))
        sheet.addRow(listOf(XlsxCell.Blank(1), null, XlsxCell.Text("gap before this")))
        return workbook
    }

    @Test
    fun theZipContainsEveryRequiredPartAndIsWellFormed() {
        val parts = unzip(simpleWorkbook().toBytes())
        listOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml"
        ).forEach { assertTrue("missing $it", it in parts) }
        assertWellFormed(parts)
    }

    @Test
    fun specialCharactersAreEscapedAndControlCharactersDropped() {
        val workbook = XlsxWorkbook(tinyStyles)
        workbook.addSheet("S").addRow(listOf(XlsxCell.Text("a & b < c > d \" e \u0001 f")))
        val sheet = unzip(workbook.toBytes()).getValue("xl/worksheets/sheet1.xml")

        assertTrue(sheet.contains("a &amp; b &lt; c &gt; d &quot; e  f"))
        parse(sheet)
    }

    @Test
    fun cellsAreAddressedByColumnAndRowIncludingGaps() {
        val sheet = unzip(simpleWorkbook().toBytes()).getValue("xl/worksheets/sheet1.xml")

        assertTrue(sheet.contains("""<c r="A1""""))
        assertTrue(sheet.contains("<f>SUM(B2:B2)</f><v>12.5</v>"))
        // The gap in row 4 means the text lands in column C, not B.
        assertTrue(sheet.contains("""<c r="C4""""))
        assertFalse(sheet.contains("""<c r="B4""""))
    }

    @Test
    fun rowAndColumnSettingsAreWritten() {
        val workbook = XlsxWorkbook(tinyStyles)
        val sheet = workbook.addSheet("S")
        sheet.showGridLines = false
        sheet.columns += XlsxColumn(2, 3, 9.5, hidden = true, outlineLevel = 1)
        sheet.merges += "A1:B1"
        sheet.negativeHighlights += "A2:B2"
        sheet.addRow(listOf(XlsxCell.Text("x")), height = 6.0)
        sheet.addRow(emptyList(), hidden = true, outlineLevel = 1)
        val xml = unzip(workbook.toBytes()).getValue("xl/worksheets/sheet1.xml")

        assertTrue(xml.contains("""showGridLines="0""""))
        assertTrue(xml.contains("""<col min="2" max="3" width="9.5" customWidth="1" hidden="1" outlineLevel="1"/>"""))
        assertTrue(xml.contains("""ht="6.0" customHeight="1""""))
        assertTrue(xml.contains("""<row r="2" hidden="1" outlineLevel="1">"""))
        assertTrue(xml.contains("""outlineLevelRow="1""""))
        assertTrue(xml.contains("""<mergeCell ref="A1:B1"/>"""))
        assertTrue(xml.contains("""sqref="A2:B2""""))
        parse(xml)
    }

    @Test
    fun columnLettersRollOverAfterZ() {
        assertEquals("A", columnName(0))
        assertEquals("M", columnName(12))
        assertEquals("Z", columnName(25))
        assertEquals("AA", columnName(26))
        assertEquals("AI", columnName(34))
    }

    @Test
    fun sheetNamesAreMadeValidForExcel() {
        assertEquals("a b c d e f", XlsxSheet("a[b]c:d*e?f").name)
        assertEquals(31, XlsxSheet("x".repeat(60)).name.length)
        assertEquals("Sheet", XlsxSheet("").name)
    }

    @Test
    fun styleSheetsShareEqualLooksAndDeclareTheirNumberFormats() {
        val styles = XlsxStyleSheet()
        val a = styles.style(XlsxFont(bold = true), numberFormat = "\"£\"#,##0.00")
        val b = styles.style(XlsxFont(bold = true), numberFormat = "\"£\"#,##0.00")
        val c = styles.style(XlsxFont(bold = true), fill = "FF112233")
        assertEquals(a, b)
        assertTrue(a != c)

        val xml = styles.toXml()
        parse(xml)
        assertTrue(xml.contains("""numFmtId="164" formatCode="&quot;£&quot;#,##0.00""""))
        // The plain default format, plus the two distinct looks (a and b are the same one).
        assertTrue(xml.contains("""<cellXfs count="3">"""))
        assertTrue(xml.contains("<dxfs count=\"1\">"))
    }

    @Test
    fun moneyFormatsUseTheCurrencySymbolAndDecimals() {
        assertEquals("\"X\"#,##0", ReportFormats.moneyFormat("X", 0))
        assertEquals("\"£\"#,##0.00", ReportFormats.moneyFormat("£", 2))
        assertEquals("£", ReportFormats.symbolFor("GBP", Locale.UK))
        assertEquals("XXX-not-real", ReportFormats.symbolFor("XXX-not-real", Locale.UK))
    }

    // --- the annual report workbook ---

    private fun sampleReport(): ByteArray {
        val groceries = Category("c1", "Groceries", TransactionType.EXPENSE)
        val rent = Category("c3", "Rent", TransactionType.EXPENSE)
        val salary = Category("c2", "Salary", TransactionType.INCOME)
        val invest = Category("c4", "ISA", TransactionType.SAVING)
        fun tx(id: String, category: Category, type: TransactionType, amount: Double, date: LocalDate, merchant: Merchant? = null, recurring: Boolean = false) =
            Transaction(
                id = id, transactionType = type, amount = amount, date = date, cadenceUnit = CadenceUnit.MONTHS,
                cadenceValue = 0, category = category, merchant = merchant,
                isRecurring = recurring, recurringGroupId = null, notes = "a <note>", tags = setOf("weekly")
            )
        val report = buildAnnualReport(
            year = 2026, payCycleStartDay = 1, currencyCode = "EUR", fractionDigits = 2,
            transactions = listOf(
                tx("t1", groceries, TransactionType.EXPENSE, 64.2, LocalDate.of(2026, 3, 15), Merchant("m1", "Lidl & Co")),
                tx("t2", rent, TransactionType.EXPENSE, 800.0, LocalDate.of(2026, 3, 1), recurring = true),
                tx("t3", salary, TransactionType.INCOME, 3200.0, LocalDate.of(2026, 3, 1)),
                tx("t4", invest, TransactionType.SAVING, 200.0, LocalDate.of(2026, 3, 2))
            ),
            startingBalance = 1000.0
        )
        return AnnualReportWorkbook.build(report, Locale.ENGLISH).toBytes()
    }

    @Test
    fun theReportHasTheTemplatesFiveSheetsInOrderAndIsWellFormed() {
        val parts = unzip(sampleReport())
        assertWellFormed(parts)

        val workbook = parts.getValue("xl/workbook.xml")
        val names = Regex("""<sheet name="([^"]+)"""").findAll(workbook).map { it.groupValues[1] }.toList()
        assertEquals(listOf("Setup", "Expenses", "Income", "Summary", "Spendings"), names)
        assertTrue(workbook.contains("""<definedName name="StartingBalance">Setup!${'$'}C${'$'}13</definedName>"""))
    }

    @Test
    fun theStartingBalanceIsTheSavingsBalanceAtRow13() {
        val setup = unzip(sampleReport()).getValue("xl/worksheets/sheet1.xml")
        assertTrue(setup.contains("Starting balance:"))
        // Row 13, column C, as "Row 13" in the instructions and the StartingBalance name say.
        assertTrue(Regex("""<c r="C13" s="\d+"><v>1000</v>""").containsMatchIn(setup))
        assertTrue(setup.contains("""<c r="C14""""))
    }

    @Test
    fun expensesListsOnlyRecurringExpensesAndLeftoverCountsBoth() {
        val parts = unzip(sampleReport())
        val expenses = parts.getValue("xl/worksheets/sheet2.xml")
        // Row 3 total, 4 savings, 5 leftover, 6 group, 7 the one recurring category (Rent). March is column F.
        assertTrue(expenses.contains("<f>F6</f><v>800"))
        assertTrue(Regex("""<c r="F4" s="\d+"><v>200</v>""").containsMatchIn(expenses))
        // Leftover = income - recurring - once-off (Spendings, March is N4) - savings.
        assertTrue(expenses.contains("<f>Income!F3-F3-Spendings!N4-F4</f>"))
        assertTrue(expenses.contains("<f>SUM(F7:F7)</f><v>800</v>"))
        assertTrue(expenses.contains(">Rent<"))
        assertFalse("a once-off expense is not listed here", expenses.contains(">Groceries<"))

        val income = parts.getValue("xl/worksheets/sheet3.xml")
        assertTrue(income.contains("<f>SUM(F4:F4)</f><v>3200</v>"))
    }

    @Test
    fun theSummaryPullsFromTheOtherSheetsAndHasAChartOfCategories() {
        val parts = unzip(sampleReport())
        val summary = parts.getValue("xl/worksheets/sheet4.xml")

        assertTrue(summary.contains("Summary - End Of Month"))
        // The running savings balance starts from the starting balance (1000) and adds the March saving (200).
        assertTrue(summary.contains("<f>StartingBalance+D10</f><v>1000</v>"))
        assertTrue(summary.contains("<f>E11+F10</f><v>1200</v>"))
        assertTrue(summary.contains("<f>Expenses!F4</f>"))
        assertTrue(summary.contains("<f>Income!F3</f><v>3200</v>"))
        // Overall expenses are recurring (800) plus once-off (64.2); once-off also has its own line.
        assertTrue(summary.contains("<f>Expenses!F3+Spendings!N4</f><v>864.2"))
        assertTrue(summary.contains("Once-off spending"))
        assertTrue(summary.contains("""<drawing r:id="rId1"/>"""))

        val chart = parts.getValue("xl/charts/chart1.xml")
        // One income category, so Overall is row 21 and the leftover row 22. Rent is row 23 and
        // once-off spending row 24, and the chart covers both.
        assertTrue(chart.contains("Summary!${'$'}C${'$'}23:${'$'}C${'$'}24"))
        assertTrue(chart.contains("Summary!${'$'}P${'$'}23:${'$'}P${'$'}24"))
        assertTrue(chart.contains("Average € spent per category"))
        assertTrue("sheet4 rels", "xl/worksheets/_rels/sheet4.xml.rels" in parts)
    }

    @Test
    fun spendingsListsOnlyOnceOffExpensesWithJanuaryStartingAtJ() {
        val spendings = unzip(sampleReport()).getValue("xl/worksheets/sheet5.xml")

        // March is the third pair of columns: N (item) and O (amount).
        assertTrue(spendings.contains("15 Mar - Lidl &amp; Co (a &lt;note&gt;)"))
        assertTrue(Regex("""<c r="O6" s="\d+"><v>64.2</v>""").containsMatchIn(spendings))
        assertTrue(spendings.contains("<f>SUM(O6:O200)</f><v>64.2"))
        assertTrue(spendings.contains("""<mergeCell ref="N1:O1"/>"""))
        // Recurring expenses, income and savings are not spendings.
        assertFalse(spendings.contains("Rent"))
        assertFalse(spendings.contains("Salary"))
    }

    @Test
    fun everyStyleTheReportUsesExistsInTheStyleSheet() {
        val parts = unzip(sampleReport())
        val styleCount = Regex("""<cellXfs count="(\d+)"""").find(parts.getValue("xl/styles.xml"))!!.groupValues[1].toInt()
        (1..5).forEach { n ->
            val used = Regex(""" s="(\d+)"""").findAll(parts.getValue("xl/worksheets/sheet$n.xml"))
                .map { it.groupValues[1].toInt() }.max()
            assertTrue("sheet$n uses style $used but the style sheet has $styleCount", used < styleCount)
        }
    }

    @Test
    fun anEmptyYearStillProducesAValidWorkbook() {
        val report = buildAnnualReport(2026, 1, "EUR", 2, emptyList())
        val parts = unzip(AnnualReportWorkbook.build(report, Locale.ENGLISH).toBytes())
        assertWellFormed(parts)
        assertFalse("no categories, so no chart", "xl/charts/chart1.xml" in parts)
    }
}
