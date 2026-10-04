package com.example.personalfinances.domain.model

/** Outcome of a backup export or import. */
sealed class BackupResult {
    /** The operation finished; the counts are how many of each item the backup file holds. */
    data class Success(
        val transactions: Int,
        val categories: Int,
        val merchants: Int
    ) : BackupResult()

    /** The operation failed and changed nothing; [message] is safe to show to the user. */
    data class Failure(val message: String) : BackupResult()
}
