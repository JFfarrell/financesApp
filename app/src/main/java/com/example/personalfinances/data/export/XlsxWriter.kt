package com.example.personalfinances.data.export

import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A minimal writer for Excel (.xlsx) workbooks, with no dependencies.
 *
 * An .xlsx file is a zip of XML files. This writes the subset needed to reproduce a designed
 * spreadsheet: sheets of text, numbers and formulas; row heights, hidden and grouped rows; column
 * widths, hidden and grouped columns; merged cells; hidden grid lines; a frozen header; a
 * "highlight negatives" rule; and one bar chart per sheet.
 *
 * The look of the cells is not decided here. The workbook is given the full text of a
 * `styles.xml` and each cell names a style by its index in that file, so the spreadsheet's fonts,
 * colours, borders and number formats come from a template. Text is stored inline (no
 * shared-strings table), which Excel, LibreOffice, Google Sheets and Numbers all read. Formulas
 * are written together with their calculated value, so viewers that do not calculate (previews)
 * still show numbers, and the workbook is flagged to recalculate when opened.
 */

/** One cell's content and style; [style] is an index into the workbook's `styles.xml` cell formats. */
sealed class XlsxCell {
    abstract val style: Int

    data class Text(val text: String, override val style: Int = 0) : XlsxCell()
    data class Number(val value: Double, override val style: Int = 0) : XlsxCell()

    /** A cell with no content that still carries a style, such as a coloured band. */
    data class Blank(override val style: Int) : XlsxCell()

    /**
     * A formula without the leading "=", and the value it currently evaluates to. Set
     * [blankWhenZero] for a formula written to return an empty string instead of zero, so the
     * stored result matches what the formula gives.
     */
    data class Formula(
        val formula: String,
        val cachedValue: Double,
        override val style: Int = 0,
        val blankWhenZero: Boolean = false
    ) : XlsxCell()
}

/** A row of cells (null leaves a gap) and how the row itself looks. */
class XlsxRow(
    val cells: List<XlsxCell?>,
    val height: Double? = null,
    val outlineLevel: Int = 0,
    val hidden: Boolean = false
)

/** Width and visibility for columns [first] to [last] (both one-based and inclusive). */
data class XlsxColumn(
    val first: Int,
    val last: Int,
    val width: Double,
    val hidden: Boolean = false,
    val outlineLevel: Int = 0
)

/**
 * A single-series column chart. [categories] and [values] are ranges such as `Summary!$C$5:$C$9`;
 * the chart is placed below row [anchorRow] (zero-based, so it is also the number of rows above it).
 */
data class XlsxChart(
    val title: String,
    val categories: String,
    val values: String,
    val anchorRow: Int
)

/** A worksheet: rows of cells plus layout settings. */
class XlsxSheet(name: String) {
    /** Excel limits names to 31 characters and forbids `[ ] : * ? / \`. */
    val name: String = name.map { if (it in "[]:*?/\\") ' ' else it }.joinToString("").take(31).trim().ifEmpty { "Sheet" }

    val rows = mutableListOf<XlsxRow>()
    val columns = mutableListOf<XlsxColumn>()

    /** Merged ranges such as `B2:C2`. */
    val merges = mutableListOf<String>()

    /** Ranges (such as `D10:O12`) whose negative numbers are shown with the styles file's first differential format. */
    val negativeHighlights = mutableListOf<String>()

    var showGridLines: Boolean = true
    var frozenRows: Int = 0
    var frozenColumns: Int = 0
    var chart: XlsxChart? = null

    /** Adds a row and returns its one-based row number. */
    fun addRow(
        cells: List<XlsxCell?>,
        height: Double? = null,
        outlineLevel: Int = 0,
        hidden: Boolean = false
    ): Int {
        rows += XlsxRow(cells, height, outlineLevel, hidden)
        return rows.size
    }
}

/**
 * A workbook of [sheets] whose cell styles come from [stylesXml], the complete text of a
 * `styles.xml` part. [definedNames] maps a name to the range it stands for, for example
 * `StartingBalance` to `Setup!$C$13`.
 */
class XlsxWorkbook(private val stylesXml: String) {
    val sheets = mutableListOf<XlsxSheet>()
    val definedNames = linkedMapOf<String, String>()

    fun addSheet(name: String): XlsxSheet = XlsxSheet(name).also { sheets += it }

    /** The workbook as the bytes of an .xlsx file. */
    fun toBytes(): ByteArray {
        require(sheets.isNotEmpty()) { "A workbook needs at least one sheet." }
        val charted = sheets.withIndex().filter { it.value.chart != null }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            // The content-types part conventionally comes first.
            zip.put("[Content_Types].xml", contentTypes(charted.size))
            zip.put("_rels/.rels", rootRelationships())
            zip.put("xl/workbook.xml", workbook())
            zip.put("xl/_rels/workbook.xml.rels", workbookRelationships())
            zip.put("xl/styles.xml", stylesXml)
            sheets.forEachIndexed { i, sheet ->
                val chartNumber = charted.indexOfFirst { it.index == i } + 1
                zip.put("xl/worksheets/sheet${i + 1}.xml", worksheet(sheet, hasChart = chartNumber > 0))
            }
            charted.forEachIndexed { n, (sheetIndex, sheet) ->
                val number = n + 1
                zip.put(
                    "xl/worksheets/_rels/sheet${sheetIndex + 1}.xml.rels",
                    relationships("rId1" to ("drawing" to "../drawings/drawing$number.xml"))
                )
                zip.put("xl/drawings/drawing$number.xml", drawing(sheet.chart!!))
                zip.put(
                    "xl/drawings/_rels/drawing$number.xml.rels",
                    relationships("rId1" to ("chart" to "../charts/chart$number.xml"))
                )
                zip.put("xl/charts/chart$number.xml", chart(sheet.chart!!))
            }
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.put(path: String, xml: String) {
        putNextEntry(ZipEntry(path))
        write(xml.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun relationships(vararg entries: Pair<String, Pair<String, String>>) = xml(
        """<Relationships xmlns="$RELATIONSHIPS_NS">""" +
            entries.joinToString("") { (id, typeAndTarget) ->
                """<Relationship Id="$id" Type="$OFFICE_RELATIONSHIPS/${typeAndTarget.first}" Target="${typeAndTarget.second}"/>"""
            } +
            "</Relationships>"
    )

    private fun contentTypes(chartCount: Int) = xml(
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            sheets.indices.joinToString("") {
                """<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
            } +
            (1..chartCount).joinToString("") {
                """<Override PartName="/xl/drawings/drawing$it.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>""" +
                    """<Override PartName="/xl/charts/chart$it.xml" ContentType="application/vnd.openxmlformats-officedocument.drawingml.chart+xml"/>"""
            } +
            "</Types>"
    )

    private fun rootRelationships() = relationships("rId1" to ("officeDocument" to "xl/workbook.xml"))

    private fun workbook() = xml(
        """<workbook xmlns="$MAIN_NS" xmlns:r="$OFFICE_RELATIONSHIPS">""" +
            "<sheets>" +
            sheets.mapIndexed { i, s -> """<sheet name="${escape(s.name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }.joinToString("") +
            "</sheets>" +
            (if (definedNames.isEmpty()) "" else
                "<definedNames>" +
                    definedNames.entries.joinToString("") { (name, ref) -> """<definedName name="${escape(name)}">${escape(ref)}</definedName>""" } +
                    "</definedNames>") +
            """<calcPr calcId="191029" fullCalcOnLoad="1"/>""" +
            "</workbook>"
    )

    private fun workbookRelationships() = xml(
        """<Relationships xmlns="$RELATIONSHIPS_NS">""" +
            sheets.indices.joinToString("") {
                """<Relationship Id="rId${it + 1}" Type="$OFFICE_RELATIONSHIPS/worksheet" Target="worksheets/sheet${it + 1}.xml"/>"""
            } +
            """<Relationship Id="rId${sheets.size + 1}" Type="$OFFICE_RELATIONSHIPS/styles" Target="styles.xml"/>""" +
            "</Relationships>"
    )

    private fun worksheet(sheet: XlsxSheet, hasChart: Boolean): String {
        val sb = StringBuilder()
        sb.append("""<worksheet xmlns="$MAIN_NS" xmlns:r="$OFFICE_RELATIONSHIPS">""")
        // Group summaries sit above their rows and to the left of their columns.
        sb.append("""<sheetPr><outlinePr summaryBelow="0" summaryRight="0"/></sheetPr>""")

        sb.append("""<sheetViews><sheetView""")
        if (!sheet.showGridLines) sb.append(""" showGridLines="0"""")
        sb.append(""" workbookViewId="0">""")
        if (sheet.frozenRows > 0 || sheet.frozenColumns > 0) {
            val topLeft = "${columnName(sheet.frozenColumns)}${sheet.frozenRows + 1}"
            val pane = when {
                sheet.frozenRows > 0 && sheet.frozenColumns > 0 -> "bottomRight"
                sheet.frozenRows > 0 -> "bottomLeft"
                else -> "topRight"
            }
            sb.append("<pane")
            if (sheet.frozenColumns > 0) sb.append(""" xSplit="${sheet.frozenColumns}"""")
            if (sheet.frozenRows > 0) sb.append(""" ySplit="${sheet.frozenRows}"""")
            sb.append(""" topLeftCell="$topLeft" activePane="$pane" state="frozen"/>""")
        }
        sb.append("</sheetView></sheetViews>")

        val maxRowLevel = sheet.rows.maxOfOrNull { it.outlineLevel } ?: 0
        val maxColumnLevel = sheet.columns.maxOfOrNull { it.outlineLevel } ?: 0
        sb.append("""<sheetFormatPr defaultColWidth="$DEFAULT_COLUMN_WIDTH" defaultRowHeight="$DEFAULT_ROW_HEIGHT" customHeight="1"""")
        if (maxColumnLevel > 0) sb.append(""" outlineLevelCol="$maxColumnLevel"""")
        if (maxRowLevel > 0) sb.append(""" outlineLevelRow="$maxRowLevel"""")
        sb.append("/>")

        if (sheet.columns.isNotEmpty()) {
            sb.append("<cols>")
            sheet.columns.forEach { c ->
                sb.append("""<col min="${c.first}" max="${c.last}" width="${c.width}" customWidth="1"""")
                if (c.hidden) sb.append(""" hidden="1"""")
                if (c.outlineLevel > 0) sb.append(""" outlineLevel="${c.outlineLevel}"""")
                sb.append("/>")
            }
            sb.append("</cols>")
        }

        sb.append("<sheetData>")
        sheet.rows.forEachIndexed { r, row ->
            val rowNumber = r + 1
            sb.append("""<row r="$rowNumber"""")
            if (row.height != null) sb.append(""" ht="${row.height}" customHeight="1"""")
            if (row.hidden) sb.append(""" hidden="1"""")
            if (row.outlineLevel > 0) sb.append(""" outlineLevel="${row.outlineLevel}"""")
            sb.append(">")
            row.cells.forEachIndexed { c, cell ->
                if (cell != null) sb.append(cellXml("${columnName(c)}$rowNumber", cell))
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")

        if (sheet.merges.isNotEmpty()) {
            sb.append("""<mergeCells count="${sheet.merges.size}">""")
            sheet.merges.forEach { sb.append("""<mergeCell ref="$it"/>""") }
            sb.append("</mergeCells>")
        }
        sheet.negativeHighlights.forEachIndexed { i, range ->
            sb.append(
                """<conditionalFormatting sqref="$range"><cfRule type="cellIs" dxfId="0" priority="${i + 1}" operator="lessThan"><formula>0</formula></cfRule></conditionalFormatting>"""
            )
        }
        if (hasChart) sb.append("""<drawing r:id="rId1"/>""")
        sb.append("</worksheet>")
        return xml(sb.toString())
    }

    private fun cellXml(ref: String, cell: XlsxCell): String = when (cell) {
        is XlsxCell.Text ->
            """<c r="$ref" s="${cell.style}" t="inlineStr"><is><t xml:space="preserve">${escape(cell.text)}</t></is></c>"""
        is XlsxCell.Number ->
            """<c r="$ref" s="${cell.style}"><v>${number(cell.value)}</v></c>"""
        is XlsxCell.Blank ->
            """<c r="$ref" s="${cell.style}"/>"""
        is XlsxCell.Formula ->
            if (cell.blankWhenZero && cell.cachedValue == 0.0) {
                """<c r="$ref" s="${cell.style}" t="str"><f>${escape(cell.formula)}</f><v></v></c>"""
            } else {
                """<c r="$ref" s="${cell.style}"><f>${escape(cell.formula)}</f><v>${number(cell.cachedValue)}</v></c>"""
            }
    }

    /** Where the chart sits: one cell anchor below the data, with a fixed size. */
    private fun drawing(chart: XlsxChart) = xml(
        """<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="$DRAWING_NS" xmlns:r="$OFFICE_RELATIONSHIPS" xmlns:c="$CHART_NS">""" +
            """<xdr:oneCellAnchor><xdr:from><xdr:col>0</xdr:col><xdr:colOff>409575</xdr:colOff>""" +
            """<xdr:row>${chart.anchorRow}</xdr:row><xdr:rowOff>190500</xdr:rowOff></xdr:from>""" +
            """<xdr:ext cx="12144375" cy="3390900"/>""" +
            """<xdr:graphicFrame macro=""><xdr:nvGraphicFramePr><xdr:cNvPr id="2" name="Chart 1"/><xdr:cNvGraphicFramePr/></xdr:nvGraphicFramePr>""" +
            """<xdr:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/></xdr:xfrm>""" +
            """<a:graphic><a:graphicData uri="$CHART_NS"><c:chart r:id="rId1"/></a:graphicData></a:graphic></xdr:graphicFrame>""" +
            """<xdr:clientData/></xdr:oneCellAnchor></xdr:wsDr>"""
    )

    private fun chart(chart: XlsxChart) = xml(
        """<c:chartSpace xmlns:a="$DRAWING_NS" xmlns:c="$CHART_NS" xmlns:r="$OFFICE_RELATIONSHIPS"><c:chart>""" +
            """<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:pPr><a:defRPr b="1" sz="1200"><a:solidFill><a:srgbClr val="434343"/></a:solidFill></a:defRPr></a:pPr>""" +
            """<a:r><a:rPr lang="en-GB" b="1" sz="1200"><a:solidFill><a:srgbClr val="434343"/></a:solidFill></a:rPr><a:t>${escape(chart.title)}</a:t></a:r></a:p></c:rich></c:tx><c:overlay val="0"/></c:title>""" +
            """<c:autoTitleDeleted val="0"/>""" +
            """<c:plotArea><c:layout/><c:barChart><c:barDir val="col"/><c:grouping val="clustered"/><c:varyColors val="0"/>""" +
            """<c:ser><c:idx val="0"/><c:order val="0"/>""" +
            """<c:spPr><a:solidFill><a:srgbClr val="FF9900"/></a:solidFill></c:spPr><c:invertIfNegative val="0"/>""" +
            """<c:cat><c:strRef><c:f>${escape(chart.categories)}</c:f></c:strRef></c:cat>""" +
            """<c:val><c:numRef><c:f>${escape(chart.values)}</c:f></c:numRef></c:val></c:ser>""" +
            """<c:axId val="960809805"/><c:axId val="955236179"/></c:barChart>""" +
            """<c:catAx><c:axId val="960809805"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="b"/><c:numFmt formatCode="General" sourceLinked="0"/><c:majorTickMark val="none"/><c:minorTickMark val="none"/><c:tickLblPos val="nextTo"/><c:crossAx val="955236179"/><c:crosses val="autoZero"/><c:auto val="1"/><c:lblAlgn val="ctr"/><c:lblOffset val="100"/><c:noMultiLvlLbl val="0"/></c:catAx>""" +
            """<c:valAx><c:axId val="955236179"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/><c:axPos val="l"/><c:majorGridlines><c:spPr><a:ln w="6350"><a:solidFill><a:srgbClr val="D9D9D9"/></a:solidFill></a:ln></c:spPr></c:majorGridlines>""" +
            """<c:numFmt formatCode="#,##0" sourceLinked="0"/><c:majorTickMark val="none"/><c:minorTickMark val="none"/><c:tickLblPos val="nextTo"/><c:spPr><a:ln><a:noFill/></a:ln></c:spPr><c:crossAx val="960809805"/><c:crosses val="autoZero"/><c:crossBetween val="between"/></c:valAx>""" +
            """</c:plotArea><c:plotVisOnly val="1"/><c:dispBlanksAs val="gap"/></c:chart></c:chartSpace>"""
    )

    private fun xml(body: String) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>$body"""

    private fun number(value: Double): String =
        if (value.isNaN() || value.isInfinite()) "0" else BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

    private companion object {
        const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        const val RELATIONSHIPS_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
        const val OFFICE_RELATIONSHIPS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        const val DRAWING_NS = "http://schemas.openxmlformats.org/drawingml/2006/main"
        const val CHART_NS = "http://schemas.openxmlformats.org/drawingml/2006/chart"
        const val DEFAULT_COLUMN_WIDTH = "12.63"
        const val DEFAULT_ROW_HEIGHT = "15.75"
    }
}

/** Escapes XML special characters and drops characters XML 1.0 cannot represent. */
internal fun escape(text: String): String = buildString(text.length) {
    for (ch in text) {
        when {
            ch == '&' -> append("&amp;")
            ch == '<' -> append("&lt;")
            ch == '>' -> append("&gt;")
            ch == '"' -> append("&quot;")
            ch.code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r' -> Unit
            else -> append(ch)
        }
    }
}

/** Column letters for a zero-based [index]: 0 is A, 25 is Z, 26 is AA. */
fun columnName(index: Int): String {
    var n = index
    val sb = StringBuilder()
    do {
        sb.append('A' + n % 26)
        n = n / 26 - 1
    } while (n >= 0)
    return sb.reverse().toString()
}
