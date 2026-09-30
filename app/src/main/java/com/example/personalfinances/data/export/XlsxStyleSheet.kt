package com.example.personalfinances.data.export

/** A font for a cell format: [color] is an ARGB hex string such as `FF374151`. */
data class XlsxFont(
    val size: Double = 10.0,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val color: String = "FF374151"
)

/** Thin lines drawn above and below a cell, as ARGB hex colours (null for no line). */
data class XlsxBorder(val top: String? = null, val bottom: String? = null)

/** Horizontal alignment of a cell's content. */
enum class XlsxAlign(val xml: String?) { DEFAULT(null), LEFT("left"), CENTER("center"), RIGHT("right") }

/**
 * Builds the `styles.xml` for a workbook from the cell formats a report asks for.
 *
 * Call [style] for each look a cell needs and use the returned index as the cell's style; equal
 * looks share one index. Number formats are given as Excel format codes (for example
 * `"£"#,##0.00`). Text is always vertically centred. The sheet also carries one differential
 * format (index 0) that turns text red, which a sheet's "highlight negatives" rule uses.
 */
class XlsxStyleSheet {
    private val fonts = mutableListOf(XlsxFont(size = 11.0, color = "FF000000"))
    private val fills = mutableListOf<String?>(null, null) // the two fills every file must start with
    private val borders = mutableListOf(XlsxBorder())
    private val numberFormats = linkedMapOf<String, Int>()
    private val formats = mutableListOf(Format(0, 0, 0, 0, XlsxAlign.DEFAULT, false, 0))

    private data class Format(
        val font: Int, val fill: Int, val border: Int, val numberFormat: Int,
        val align: XlsxAlign, val wrap: Boolean, val indent: Int
    )

    /** Returns the index of a cell format with these properties, adding it if it is new. */
    fun style(
        font: XlsxFont = XlsxFont(),
        fill: String? = null,
        border: XlsxBorder = XlsxBorder(),
        numberFormat: String? = null,
        align: XlsxAlign = XlsxAlign.DEFAULT,
        wrap: Boolean = false,
        indent: Int = 0
    ): Int {
        val format = Format(
            font = indexOf(fonts, font),
            fill = if (fill == null) 0 else indexOf(fills, fill),
            border = indexOf(borders, border),
            numberFormat = numberFormat?.let { numberFormats.getOrPut(it) { FIRST_CUSTOM_FORMAT + numberFormats.size } } ?: 0,
            align = align, wrap = wrap, indent = indent
        )
        return indexOf(formats, format)
    }

    private fun <T> indexOf(list: MutableList<T>, item: T): Int {
        val existing = list.indexOf(item)
        if (existing >= 0) return existing
        list += item
        return list.size - 1
    }

    /** The complete text of the `styles.xml` part. */
    fun toXml(): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        if (numberFormats.isNotEmpty()) {
            sb.append("""<numFmts count="${numberFormats.size}">""")
            numberFormats.forEach { (code, id) -> sb.append("""<numFmt numFmtId="$id" formatCode="${escape(code)}"/>""") }
            sb.append("</numFmts>")
        }

        sb.append("""<fonts count="${fonts.size}">""")
        fonts.forEach { f ->
            sb.append("<font>")
            if (f.bold) sb.append("<b/>")
            if (f.italic) sb.append("<i/>")
            sb.append("""<sz val="${f.size}"/><color rgb="${f.color}"/><name val="Calibri"/><family val="2"/></font>""")
        }
        sb.append("</fonts>")

        sb.append("""<fills count="${fills.size}">""")
        fills.forEachIndexed { i, color ->
            when {
                i == 0 -> sb.append("""<fill><patternFill patternType="none"/></fill>""")
                i == 1 -> sb.append("""<fill><patternFill patternType="gray125"/></fill>""")
                else -> sb.append("""<fill><patternFill patternType="solid"><fgColor rgb="$color"/><bgColor indexed="64"/></patternFill></fill>""")
            }
        }
        sb.append("</fills>")

        sb.append("""<borders count="${borders.size}">""")
        borders.forEach { b ->
            sb.append("<border><left/><right/>")
            sb.append(if (b.top == null) "<top/>" else """<top style="thin"><color rgb="${b.top}"/></top>""")
            sb.append(if (b.bottom == null) "<bottom/>" else """<bottom style="thin"><color rgb="${b.bottom}"/></bottom>""")
            sb.append("<diagonal/></border>")
        }
        sb.append("</borders>")

        sb.append("""<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""")
        sb.append("""<cellXfs count="${formats.size}">""")
        formats.forEach { f ->
            sb.append("""<xf numFmtId="${f.numberFormat}" fontId="${f.font}" fillId="${f.fill}" borderId="${f.border}" xfId="0"""")
            sb.append(""" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">""")
            sb.append("<alignment")
            f.align.xml?.let { sb.append(""" horizontal="$it"""") }
            sb.append(""" vertical="center"""")
            if (f.wrap) sb.append(""" wrapText="1"""")
            if (f.indent > 0) sb.append(""" indent="${f.indent}"""")
            sb.append("/></xf>")
        }
        sb.append("</cellXfs>")

        sb.append("""<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""")
        sb.append("""<dxfs count="1"><dxf><font><color rgb="FFB91C1C"/></font></dxf></dxfs>""")
        sb.append("</styleSheet>")
        return sb.toString()
    }

    private companion object {
        /** Custom number formats are numbered from 164; lower numbers are built in. */
        const val FIRST_CUSTOM_FORMAT = 164
    }
}
