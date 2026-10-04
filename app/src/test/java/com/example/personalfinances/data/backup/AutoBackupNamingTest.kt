package com.example.personalfinances.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Plain JVM tests for automatic backup file naming and pruning. Pruning deletes files, so the
 * important properties are what it keeps and that it never touches anything it did not create.
 * Run with `./gradlew :app:testDebugUnitTest`.
 */
class AutoBackupNamingTest {

    private fun names(vararg dates: String) = dates.map { "personal-wallot-auto-$it.json" }

    @Test
    fun theFileNameForADateIsTheDatedName() {
        assertEquals("personal-wallot-auto-2026-03-05.json", AutoBackupNaming.fileNameFor(LocalDate.of(2026, 3, 5)))
    }

    @Test
    fun onlyFilesTheFeatureCreatedAreRecognised() {
        assertTrue(AutoBackupNaming.isAutoBackupFile("personal-wallot-auto-2026-03-05.json"))
        // A manual export, a similar name, and unrelated files are not ours.
        assertFalse(AutoBackupNaming.isAutoBackupFile("personal-wallot-backup-2026-03-05.json"))
        assertFalse(AutoBackupNaming.isAutoBackupFile("personal-wallot-auto-2026-03-05.json.bak"))
        assertFalse(AutoBackupNaming.isAutoBackupFile("personal-wallot-auto-latest.json"))
        assertFalse(AutoBackupNaming.isAutoBackupFile("holiday-photos.json"))
    }

    @Test
    fun theNewestFilesAreKeptAndOlderOnesPruned() {
        val all = names("2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04", "2026-01-05")
        // Kept: the three newest, whatever order they are listed in.
        assertEquals(names("2026-01-02", "2026-01-01").sorted(), AutoBackupNaming.namesToPrune(all.shuffled(), keep = 3).sorted())
    }

    @Test
    fun nothingIsPrunedWhileWithinTheLimit() {
        val all = names("2026-01-01", "2026-01-02")
        assertTrue(AutoBackupNaming.namesToPrune(all, keep = 14).isEmpty())
        assertTrue(AutoBackupNaming.namesToPrune(emptyList()).isEmpty())
    }

    @Test
    fun otherFilesInTheFolderAreNeverPrunedAndDoNotCountTowardsTheLimit() {
        val ours = names("2026-01-01", "2026-01-02", "2026-01-03")
        val others = listOf("personal-wallot-backup-2025-12-31.json", "notes.txt", "personal-wallot-auto-latest.json")

        val pruned = AutoBackupNaming.namesToPrune(others + ours, keep = 2)

        assertEquals(names("2026-01-01"), pruned)
        assertTrue(pruned.none { it in others })
    }

    @Test
    fun datesSortCorrectlyAcrossMonthsAndYears() {
        val all = names("2025-12-31", "2026-01-01", "2026-02-01")
        assertEquals(names("2025-12-31"), AutoBackupNaming.namesToPrune(all, keep = 2))
    }

    @Test
    fun theDefaultLimitIsFourteenDays() {
        assertEquals(14, AutoBackupNaming.KEEP_LATEST)
        val fifteen = (1..15).map { "personal-wallot-auto-2026-01-%02d.json".format(it) }
        assertEquals(listOf("personal-wallot-auto-2026-01-01.json"), AutoBackupNaming.namesToPrune(fifteen))
    }
}
