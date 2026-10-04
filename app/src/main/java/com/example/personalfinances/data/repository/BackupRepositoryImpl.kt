package com.example.personalfinances.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import androidx.work.WorkManager
import com.example.personalfinances.data.backup.AutoBackupNaming
import com.example.personalfinances.data.backup.AutoBackupScheduler
import com.example.personalfinances.data.backup.BackupFile
import com.example.personalfinances.data.backup.BackupFolder
import com.example.personalfinances.data.backup.BackupFormatException
import com.example.personalfinances.data.backup.buildBackupFile
import com.example.personalfinances.data.backup.parseBackup
import com.example.personalfinances.data.local.datastore.SettingsDataStore
import com.example.personalfinances.data.local.db.AppDatabase
import com.example.personalfinances.data.local.db.dao.BackupDao
import com.example.personalfinances.domain.model.AutoBackupStatus
import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * [BackupRepository] that reads and writes JSON files through Android's content resolver, so the
 * user can choose any location the system file pickers offer.
 *
 * An import validates the whole file first and then writes everything inside one database
 * transaction, so a bad or interrupted import leaves the existing data untouched.
 *
 * Automatic backup writes into a folder the user picked once (see [BackupFolder]); the actual
 * scheduling after data changes is [AutoBackupScheduler]'s job.
 */
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val backupDao: BackupDao,
    private val settings: SettingsDataStore
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** A backup of everything as JSON text, with how many of each item it holds. */
    private suspend fun buildBackup(): Pair<String, BackupResult.Success> {
        val categories = backupDao.categories()
        val merchants = backupDao.merchants()
        val transactions = backupDao.transactions()
        val file = buildBackupFile(
            categories = categories,
            merchants = merchants,
            transactions = transactions,
            savingsGoal = backupDao.savingsGoal(),
            payCycleStartDay = settings.payCycleStartDay.first(),
            currencyCode = settings.currencyCode.first(),
            exportedAt = Instant.now().toString()
        )
        return json.encodeToString(file) to BackupResult.Success(transactions.size, categories.size, merchants.size)
    }

    override suspend fun exportTo(destination: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val (text, counts) = buildBackup()

            // "wt" truncates, so overwriting an existing, longer file leaves no stale tail.
            val output = context.contentResolver.openOutputStream(Uri.parse(destination), "wt")
                ?: return@withContext BackupResult.Failure("Could not open that file for writing.")
            output.use { it.write(text.toByteArray(Charsets.UTF_8)) }

            settings.saveLastBackupAt(System.currentTimeMillis())
            counts
        } catch (e: IOException) {
            BackupResult.Failure("Could not write the backup: ${e.message ?: "unknown error"}")
        } catch (e: SecurityException) {
            BackupResult.Failure("The app is not allowed to write to that location.")
        }
    }

    override suspend fun importFrom(source: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(Uri.parse(source))
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return@withContext BackupResult.Failure("Could not open that file.")

            val file = try {
                json.decodeFromString<BackupFile>(text)
            } catch (e: SerializationException) {
                return@withContext BackupResult.Failure("This file is not a valid Wallot backup.")
            } catch (e: IllegalArgumentException) {
                return@withContext BackupResult.Failure("This file is not a valid Wallot backup.")
            }

            val parsed = parseBackup(
                file = file,
                existingCategories = backupDao.categories(),
                existingMerchants = backupDao.merchants()
            )

            // Parents before children: transactions refer to categories and merchants.
            database.withTransaction {
                backupDao.upsertCategories(parsed.categories)
                backupDao.upsertMerchants(parsed.merchants)
                backupDao.upsertTransactions(parsed.transactions)
                parsed.savingsGoal?.let { backupDao.upsertSavingsGoal(it) }
            }
            parsed.payCycleStartDay?.let { settings.savePayCycleStartDay(it) }
            parsed.currencyCode?.let { settings.saveCurrencyCode(it) }

            BackupResult.Success(parsed.transactions.size, parsed.categories.size, parsed.merchants.size)
        } catch (e: BackupFormatException) {
            BackupResult.Failure(e.message ?: "This backup could not be read.")
        } catch (e: IOException) {
            BackupResult.Failure("Could not read the file: ${e.message ?: "unknown error"}")
        } catch (e: SecurityException) {
            BackupResult.Failure("The app is not allowed to read that file.")
        }
    }

    override fun getLastBackupAt(): Flow<Long?> = settings.lastBackupAt

    override fun getAutoBackupStatus(): Flow<AutoBackupStatus> =
        combine(settings.autoBackupFolder, settings.autoBackupError) { folder, error ->
            AutoBackupStatus(
                enabled = folder != null,
                folderName = folder?.let { runCatching { BackupFolder(context, Uri.parse(it)).displayName() }.getOrNull() },
                lastError = error
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun enableAutoBackup(folder: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(folder)
            // Keep read and write access to this folder across restarts, so later backups need no prompt.
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settings.autoBackupFolder.first()
                ?.takeIf { it != folder }
                ?.let { releasePermission(it) }

            settings.saveAutoBackupFolder(folder)
            settings.saveAutoBackupError(null)
            runAutoBackup()
        } catch (e: SecurityException) {
            BackupResult.Failure("The app could not get permission to use that folder. Try another one.")
        } catch (e: Exception) {
            BackupResult.Failure("Could not turn on automatic backup: ${e.message ?: "unknown error"}")
        }
    }

    override suspend fun disableAutoBackup() = withContext(Dispatchers.IO) {
        settings.autoBackupFolder.first()?.let { releasePermission(it) }
        settings.saveAutoBackupFolder(null)
        settings.saveAutoBackupError(null)
        WorkManager.getInstance(context).cancelUniqueWork(AutoBackupScheduler.WORK_NAME)
        Unit
    }

    override suspend fun runAutoBackup(): BackupResult = withContext(Dispatchers.IO) {
        val folderAddress = settings.autoBackupFolder.first()
            ?: return@withContext BackupResult.Failure("Automatic backup is off.")
        try {
            val (text, counts) = buildBackup()
            val folder = BackupFolder(context, Uri.parse(folderAddress))

            val written = folder.write(
                AutoBackupNaming.fileNameFor(LocalDate.now()),
                text.toByteArray(Charsets.UTF_8)
            )
            if (!written) return@withContext recordFailure("Could not write to the backup folder.")

            // Keep a history of daily files, deleting only the ones this feature created.
            val entries = folder.list()
            val doomed = AutoBackupNaming.namesToPrune(entries.map { it.name }).toSet()
            entries.filter { it.name in doomed }.forEach { folder.delete(it) }

            settings.saveLastBackupAt(System.currentTimeMillis())
            settings.saveAutoBackupError(null)
            counts
        } catch (e: SecurityException) {
            recordFailure("The app no longer has permission to use the backup folder. Choose it again.")
        } catch (e: Exception) {
            recordFailure("Automatic backup failed: ${e.message ?: "unknown error"}")
        }
    }

    /** Remembers why an automatic backup failed so Settings can show it, and returns the failure. */
    private suspend fun recordFailure(message: String): BackupResult.Failure {
        settings.saveAutoBackupError(message)
        return BackupResult.Failure(message)
    }

    private fun releasePermission(folder: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(folder),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Already released or never held: nothing to do.
        }
    }
}
