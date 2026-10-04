package com.example.personalfinances.ui.screen.manage

import com.example.personalfinances.domain.model.Merchant
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Plain JVM tests for the manage screen's merchant search, filter and sort; run with `./gradlew :app:testDebugUnitTest`. */
class MerchantListOptionsTest {

    private fun merchant(name: String, usage: Int = 0, lastUsed: LocalDate? = null) =
        ManagedMerchant(Merchant(id = name, name = name), usage, lastUsed)

    private val tesco = merchant("Tesco", usage = 12, lastUsed = LocalDate.of(2026, 9, 30))
    private val aldi = merchant("aldi", usage = 3, lastUsed = LocalDate.of(2026, 10, 2))
    private val tescoExpress = merchant("Tesco Express", usage = 3, lastUsed = LocalDate.of(2026, 8, 1))
    private val oldShop = merchant("Old Shop")
    private val all = listOf(tesco, aldi, tescoExpress, oldShop)

    private fun names(list: List<ManagedMerchant>) = list.map { it.merchant.name }

    @Test
    fun nameSortIgnoresCase() {
        val result = filterAndSortMerchants(all, "", MerchantSort.NAME, unusedOnly = false)
        assertEquals(listOf("aldi", "Old Shop", "Tesco", "Tesco Express"), names(result))
    }

    @Test
    fun mostUsedPutsBusiestFirstAndBreaksTiesByName() {
        val result = filterAndSortMerchants(all, "", MerchantSort.MOST_USED, unusedOnly = false)
        assertEquals(listOf("Tesco", "aldi", "Tesco Express", "Old Shop"), names(result))
    }

    @Test
    fun recentlyUsedPutsLatestFirstAndNeverUsedLast() {
        val result = filterAndSortMerchants(all, "", MerchantSort.RECENTLY_USED, unusedOnly = false)
        assertEquals(listOf("aldi", "Tesco", "Tesco Express", "Old Shop"), names(result))
    }

    @Test
    fun searchMatchesAnywhereInTheNameIgnoringCaseAndSpaces() {
        val result = filterAndSortMerchants(all, "  ESCO ", MerchantSort.NAME, unusedOnly = false)
        assertEquals(listOf("Tesco", "Tesco Express"), names(result))
    }

    @Test
    fun unusedOnlyKeepsMerchantsWithNoTransactions() {
        val result = filterAndSortMerchants(all, "", MerchantSort.NAME, unusedOnly = true)
        assertEquals(listOf("Old Shop"), names(result))
    }

    @Test
    fun searchAndUnusedFilterCombine() {
        val result = filterAndSortMerchants(all, "tesco", MerchantSort.NAME, unusedOnly = true)
        assertEquals(emptyList<String>(), names(result))
    }
}
