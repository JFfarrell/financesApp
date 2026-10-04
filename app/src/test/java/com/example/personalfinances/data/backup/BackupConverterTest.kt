package com.example.personalfinances.data.backup

import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.data.local.db.entity.MerchantEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Plain JVM tests for the rules that decide whether a backup file can be imported, and how.
 * Run with `./gradlew :app:testDebugUnitTest`.
 */
class BackupConverterTest {

    private fun transaction(
        id: String = "t1",
        categoryId: String = "c1",
        merchantId: String? = null,
        date: String = "2026-03-15",
        tags: List<String> = emptyList()
    ) = BackupTransaction(
        id = id, type = "EXPENSE", amount = 12.5, date = date, cadenceUnit = "MONTHS", cadenceValue = 0,
        categoryId = categoryId, merchantId = merchantId, isRecurring = false, tags = tags
    )

    private fun file(
        categories: List<BackupCategory> = listOf(BackupCategory("c1", "Groceries", "EXPENSE")),
        merchants: List<BackupMerchant> = emptyList(),
        transactions: List<BackupTransaction> = listOf(transaction()),
        settings: BackupSettings = BackupSettings()
    ) = BackupFile(
        exportedAt = "2026-09-30T10:00:00Z", settings = settings,
        categories = categories, merchants = merchants, transactions = transactions
    )

    private fun parse(
        file: BackupFile,
        existingCategories: List<CategoryEntity> = emptyList(),
        existingMerchants: List<MerchantEntity> = emptyList()
    ) = parseBackup(file, existingCategories, existingMerchants)

    private fun assertRejected(file: BackupFile, existing: List<CategoryEntity> = emptyList()) {
        try {
            parse(file, existing)
            fail("Expected the backup to be rejected")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }

    @Test
    fun aValidFileParses() {
        val parsed = parse(file())
        assertEquals(1, parsed.categories.size)
        assertEquals(1, parsed.transactions.size)
        assertEquals(12.5, parsed.transactions.first().amount, 0.0)
    }

    @Test
    fun aFileFromAnotherAppIsRejected() {
        assertRejected(file().copy(app = "something-else"))
    }

    @Test
    fun aNewerFormatVersionIsRejected() {
        assertRejected(file().copy(formatVersion = BACKUP_FORMAT_VERSION + 1))
    }

    @Test
    fun aBadDateIsRejected() {
        assertRejected(file(transactions = listOf(transaction(date = "nope"))))
    }

    @Test
    fun aTransactionWithAnUnknownCategoryIsRejected() {
        assertRejected(file(transactions = listOf(transaction(categoryId = "missing"))))
    }

    @Test
    fun aTransactionMayUseACategoryAlreadyInTheApp() {
        val existing = listOf(CategoryEntity("existing", "Groceries", "EXPENSE"))
        val parsed = parse(file(categories = emptyList(), transactions = listOf(transaction(categoryId = "existing"))), existing)
        assertEquals("existing", parsed.transactions.first().categoryId)
    }

    @Test
    fun aCategoryMatchingByTypeAndNameIsReusedNotDuplicated() {
        // A fresh install seeds its own "Groceries" with a different id.
        val existing = listOf(CategoryEntity("seeded", "groceries", "EXPENSE"))
        val parsed = parse(file(), existing)

        assertTrue(parsed.categories.isEmpty())
        assertEquals("seeded", parsed.transactions.first().categoryId)
    }

    @Test
    fun theSameNameUnderADifferentTypeIsNotMerged() {
        val existing = listOf(CategoryEntity("seeded", "Groceries", "INCOME"))
        val parsed = parse(file(), existing)

        assertEquals(1, parsed.categories.size)
        assertEquals("c1", parsed.transactions.first().categoryId)
    }

    @Test
    fun aMerchantMatchingByNameIsReused() {
        val backup = file(
            merchants = listOf(BackupMerchant("m-file", "Lidl")),
            transactions = listOf(transaction(merchantId = "m-file"))
        )
        val parsed = parse(backup, existingMerchants = listOf(MerchantEntity("m-app", "LIDL")))

        assertTrue(parsed.merchants.isEmpty())
        assertEquals("m-app", parsed.transactions.first().merchantId)
    }

    @Test
    fun tagsAreNormalisedOnImport() {
        val parsed = parse(file(transactions = listOf(transaction(tags = listOf("Eating Out", "#Car", "  ")))))
        assertEquals("[\"eating-out\",\"car\"]", parsed.transactions.first().tags)
    }

    @Test
    fun anUnknownCurrencyCodeIsIgnoredAndAKnownOneKept() {
        assertNull(parse(file(settings = BackupSettings(currencyCode = "NOT-A-CODE"))).currencyCode)
        assertEquals("EUR", parse(file(settings = BackupSettings(currencyCode = "EUR"))).currencyCode)
    }
}
