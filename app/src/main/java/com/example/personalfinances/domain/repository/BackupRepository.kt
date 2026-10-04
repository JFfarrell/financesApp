package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.AutoBackupStatus
import com.example.personalfinances.domain.model.BackupResult
import kotlinx.coroutines.flow.Flow

/**
 * Saves all of the user's data to a file they choose, and reads it back.
 *
 * Locations are opaque strings (the caller passes a document address from the system file
 * picker), which keeps this interface free of Android types.
 *
 * A backup holds transactions, categories, merchants, the savings goal and the pay-cycle start
 * day. It never holds the password.
 */
interface BackupRepository {
    /** Writes a backup to [destination], replacing the file's contents, and records the time. */
    suspend fun exportTo(destination: String): BackupResult

    /**
     * Reads the backup at [source] and merges it into the app: items are added, and items with an
     * id that already exists are replaced by the file's version. Nothing is deleted. The whole
     * import succeeds or nothing changes.
     */
    suspend fun importFrom(source: String): BackupResult

    /** Emits the time of the last successful backup (manual or automatic) in epoch milliseconds, or null if none. */
    fun getLastBackupAt(): Flow<Long?>

    /** Emits whether automatic backup is on and how it is going. */
    fun getAutoBackupStatus(): Flow<AutoBackupStatus>

    /**
     * Turns automatic backup on, saving to the folder at [folder] (an address from the system
     * folder picker), and takes a first backup straight away so the user sees it working.
     */
    suspend fun enableAutoBackup(folder: String): BackupResult

    /** Turns automatic backup off and stops any backup already queued. Existing files are kept. */
    suspend fun disableAutoBackup()

    /**
     * Takes one automatic backup now: writes today's file to the chosen folder (replacing earlier
     * ones from the same day) and deletes automatic files beyond the newest 14. Records any
     * failure so the settings screen can show it.
     */
    suspend fun runAutoBackup(): BackupResult
}
