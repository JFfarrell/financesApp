package com.example.personalfinances.data.backup

import kotlinx.serialization.Serializable

/** Identifies a file as a Wallot backup. */
const val BACKUP_APP_ID = "wallot"

/**
 * Version of the backup file layout. Raise it only for a change older versions cannot read, and
 * keep reading every older version so a backup made today can always be restored.
 */
const val BACKUP_FORMAT_VERSION = 1

/**
 * The on-disk shape of a backup: plain JSON that is readable without the app.
 *
 * It is deliberately separate from the Room entities so the database can change without breaking
 * old backups. Dates are ISO text (2026-09-29), types are enum names, tags are a list, and the
 * password is never included.
 */
@Serializable
data class BackupFile(
    val app: String = BACKUP_APP_ID,
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val exportedAt: String,
    val settings: BackupSettings = BackupSettings(),
    val savingsGoal: BackupSavingsGoal? = null,
    val categories: List<BackupCategory> = emptyList(),
    val merchants: List<BackupMerchant> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList()
)

@Serializable
data class BackupSettings(val payCycleStartDay: Int = 1)

@Serializable
data class BackupSavingsGoal(val targetAmount: Double, val startingAmount: Double)

@Serializable
data class BackupCategory(val id: String, val name: String, val type: String)

@Serializable
data class BackupMerchant(val id: String, val name: String)

@Serializable
data class BackupTransaction(
    val id: String,
    val type: String,
    val amount: Double,
    val date: String,
    val cadenceUnit: String,
    val cadenceValue: Int,
    val categoryId: String,
    val merchantId: String? = null,
    val isRecurring: Boolean,
    val recurringGroupId: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList()
)
