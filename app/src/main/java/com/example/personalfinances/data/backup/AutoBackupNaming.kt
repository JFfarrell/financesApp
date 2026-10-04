package com.example.personalfinances.data.backup

import java.time.LocalDate

/**
 * File naming and retention for automatic backups.
 *
 * There is one file per day, so several changes in a day overwrite that day's file, and older days
 * are kept as a history. That history matters: a single file overwritten on every change would
 * faithfully copy an accidental mistake (such as deleting everything) over the good backup.
 *
 * Automatic files use their own name prefix, different from manual exports, so pruning can never
 * touch a backup the user made by hand or any other file in the folder.
 */
object AutoBackupNaming {
    /** How many daily files are kept; older ones are deleted after each automatic backup. */
    const val KEEP_LATEST = 14

    private val pattern = Regex("""^personal-wallot-auto-\d{4}-\d{2}-\d{2}\.json$""")

    fun fileNameFor(date: LocalDate): String = "personal-wallot-auto-$date.json"

    /** True only for files that this feature created. */
    fun isAutoBackupFile(name: String): Boolean = pattern.matches(name)

    /**
     * Which of [names] to delete: the automatic backup files beyond the newest [keep], newest
     * judged by the date in the name. Every other name is left alone.
     */
    fun namesToPrune(names: List<String>, keep: Int = KEEP_LATEST): List<String> =
        names.filter(::isAutoBackupFile).sortedDescending().drop(keep)
}
