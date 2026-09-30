package com.example.personalfinances.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.personalfinances.util.MoneyFormatter

/**
 * Provides the [MoneyFormatter] for the user's chosen currency. It is set once in `MainActivity`,
 * so every screen formats amounts the same way and updates when the setting changes:
 * `val money = LocalMoneyFormatter.current` then `money.format(amount)`.
 */
val LocalMoneyFormatter = staticCompositionLocalOf { MoneyFormatter.forCode(null) }
