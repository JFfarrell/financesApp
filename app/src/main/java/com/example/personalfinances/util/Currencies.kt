package com.example.personalfinances.util

import java.util.Currency
import java.util.Locale

/** One currency the user can choose: its ISO 4217 [code], readable [name] and display [symbol]. */
data class CurrencyOption(val code: String, val name: String, val symbol: String)

/** The currencies offered in Settings. */
object Currencies {
    /**
     * Every real currency the platform knows, sorted by name. Pseudo-currencies (such as gold or
     * "no currency" codes) have no decimal places defined and are left out.
     */
    fun all(locale: Locale = Locale.getDefault()): List<CurrencyOption> =
        Currency.getAvailableCurrencies()
            .filter { it.defaultFractionDigits >= 0 }
            .map { CurrencyOption(it.currencyCode, it.getDisplayName(locale), it.getSymbol(locale)) }
            .sortedBy { it.name.lowercase() }

    /** A one-line description for the settings row, for example "Euro (EUR)". */
    fun describe(code: String?, locale: Locale = Locale.getDefault()): String {
        val currency = MoneyFormatter.resolve(code)
        val label = "${currency.getDisplayName(locale)} (${currency.currencyCode})"
        return if (code == null) "Phone default: $label" else label
    }
}
