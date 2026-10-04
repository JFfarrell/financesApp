package com.example.personalfinances.data.export

import java.util.Currency
import java.util.Locale

/** Number formats for the annual report, in the user's currency. */
object ReportFormats {
    /** The symbol shown before amounts, such as `£`, falling back to the code itself. */
    fun symbolFor(currencyCode: String, locale: Locale): String =
        try {
            Currency.getInstance(currencyCode).getSymbol(locale)
        } catch (e: IllegalArgumentException) {
            currencyCode
        }

    /** An Excel number format such as `"£"#,##0.00` (or `"£"#,##0` for no decimals). */
    fun moneyFormat(symbol: String, fractionDigits: Int): String =
        "\"${symbol.replace("\"", "")}\"#,##0" + if (fractionDigits > 0) "." + "0".repeat(fractionDigits) else ""
}
