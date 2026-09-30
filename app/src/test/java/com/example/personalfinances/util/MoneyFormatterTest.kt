package com.example.personalfinances.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Currency

/** Plain JVM tests for currency handling; run with `./gradlew :app:testDebugUnitTest`. */
class MoneyFormatterTest {

    @Test
    fun decimalPlacesFollowTheCurrency() {
        assertEquals(2, MoneyFormatter.forCode("EUR").fractionDigits)
        assertEquals(2, MoneyFormatter.forCode("USD").fractionDigits)
        assertEquals(0, MoneyFormatter.forCode("JPY").fractionDigits)
    }

    @Test
    fun formattedAmountCarriesTheChosenCurrencySymbol() {
        val eur = MoneyFormatter.forCode("EUR")
        assertTrue(eur.format(12.5).contains(eur.symbol))
        assertFalse(MoneyFormatter.forCode("JPY").format(1500.0).contains("."))
    }

    @Test
    fun aMissingOrInvalidCodeFallsBackToTheDeviceCurrency() {
        val device = MoneyFormatter.deviceCurrency()
        assertEquals(device, MoneyFormatter.resolve(null))
        assertEquals(device, MoneyFormatter.resolve("NOT-A-CODE"))
    }

    @Test
    fun theCurrencyListHasRealCurrenciesOnly() {
        val codes = Currencies.all().map { it.code }
        assertTrue("EUR" in codes)
        assertTrue("USD" in codes)
        assertTrue("JPY" in codes)
        // Gold and "no currency" are not currencies anyone spends.
        assertFalse("XAU" in codes)
        assertFalse("XXX" in codes)
        assertEquals(Currency.getInstance("EUR").currencyCode, Currencies.all().first { it.code == "EUR" }.code)
    }
}
