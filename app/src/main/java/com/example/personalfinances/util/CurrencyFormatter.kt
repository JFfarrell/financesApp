package com.example.personalfinances.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    fun format(amount: Double): String = formatter.format(amount)

    /** The device locale's currency symbol, e.g. "€", for showing beside a typed amount. */
    val symbol: String
        get() = formatter.currency?.symbol ?: ""
}
