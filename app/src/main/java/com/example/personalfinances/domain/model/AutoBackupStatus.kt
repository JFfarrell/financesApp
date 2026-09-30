package com.example.personalfinances.domain.model

/**
 * Whether automatic backup is on, and how it is doing. [folderName] is the chosen folder's
 * readable name, and [lastError] is why the most recent automatic backup failed (null when it
 * succeeded or none has run yet).
 */
data class AutoBackupStatus(
    val enabled: Boolean = false,
    val folderName: String? = null,
    val lastError: String? = null
)
