package com.example.personalfinances.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.personalfinances.data.backup.BackupFile
import com.example.personalfinances.data.backup.BackupFormatException
import com.example.personalfinances.data.backup.buildBackupFile
import com.example.personalfinances.data.backup.parseBackup
import com.example.personalfinances.data.local.datastore.SettingsDataStore
import com.example.personalfinances.data.local.db.AppDatabase
import com.example.personalfinances.data.local.db.dao.BackupDao
import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * [BackupRepository] that reads and writes JSON files through Android's content resolver, so the
 * user can choose any location the system file picker offers.
 *
 * An import validates the whole file first and then writes everything inside one database
 * transaction, so a bad or interrupted import leaves the existing data untouched.
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

    override suspend fun exportTo(destination: String): BackupResult = withContext(Dispatchers.IO) {
        try {
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

            // "wt" truncates, so overwriting an existing, longer file leaves no stale tail.
            val output = context.contentResolver.openOutputStream(Uri.parse(destination), "wt")
                ?: return@withContext BackupResult.Failure("Could not open that file for writing.")
            output.use { it.write(json.encodeToString(file).toByteArray(Charsets.UTF_8)) }

            settings.saveLastBackupAt(System.currentTimeMillis())
            BackupResult.Success(transactions.size, categories.size, merchants.size)
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
}
