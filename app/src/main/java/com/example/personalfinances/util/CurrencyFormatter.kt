package com.example.personalfinances.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    fun format(amount: Double): String = formatter.format(amount)
}
