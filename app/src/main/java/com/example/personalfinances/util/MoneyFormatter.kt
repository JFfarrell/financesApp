package com.example.personalfinances.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats amounts of money for one currency, using the phone's locale for the layout (decimal
 * separator, symbol position). Only the currency comes from the user's setting.
 *
 * Amounts are stored as plain numbers with no currency attached, so changing the currency
 * relabels every amount but never converts it.
 */
class MoneyFormatter(val currency: Currency) {
    private val numberFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).also {
            it.currency = currency
            // setCurrency changes the symbol but not the decimal places, which would otherwise
            // stay those of the phone's own currency (showing yen as "1,500.00").
            val digits = currency.defaultFractionDigits.coerceAtLeast(0)
            it.minimumFractionDigits = digits
            it.maximumFractionDigits = digits
        }

    /** Formats [amount], for example "€1,234.50" (fraction digits follow the currency). */
    fun format(amount: Double): String = numberFormat.format(amount)

    /** The currency symbol as the phone's locale would show it, for example "€". */
    val symbol: String = currency.getSymbol(Locale.getDefault())

    /** How many decimal places this currency normally has: 2 for euros, 0 for yen. */
    val fractionDigits: Int = currency.defaultFractionDigits.coerceAtLeast(0)

    companion object {
        /**
         * The currency for a saved [code], or the phone's own currency when [code] is null or not a
         * valid code.
         */
        fun forCode(code: String?): MoneyFormatter = MoneyFormatter(resolve(code))

        /** The currency of the phone's region, falling back to US dollars if it has none. */
        fun deviceCurrency(): Currency =
            runCatching { Currency.getInstance(Locale.getDefault()) }.getOrNull()
                ?: Currency.getInstance("USD")

        fun resolve(code: String?): Currency =
            code?.let { runCatching { Currency.getInstance(it) }.getOrNull() } ?: deviceCurrency()
    }
}
