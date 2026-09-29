package com.example.personalfinances.domain.repository

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

    /** Emits the time of the last successful export in epoch milliseconds, or null if none. */
    fun getLastBackupAt(): Flow<Long?>
}
