package com.example.personalfinances.data.backup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.personalfinances.data.local.datastore.SettingsDataStore
import com.example.personalfinances.data.local.db.dao.BackupDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues an automatic backup whenever the user's data changes, while automatic backup is on.
 *
 * It watches the four tables that hold user data. Each change (re)schedules one background job to
 * run [DELAY_SECONDS] later; scheduling again replaces the pending job, so a burst of edits
 * produces a single backup shortly after the last one. The job itself is run by WorkManager
 * ([AutoBackupWorker]), so it still happens if the app is closed before the delay is up.
 *
 * One backup is also queued each time the app starts, as a safety net in case an earlier one
 * failed or was interrupted.
 */
@Singleton
class AutoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupDao: BackupDao,
    private val settings: SettingsDataStore
) {
    /** Starts watching in [scope], which should live as long as the app process. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        // Each query emits its current state first; dropping that leaves only real changes.
        val changes: Flow<Unit> = merge(
            backupDao.transactionChanges().drop(1).map { },
            backupDao.categoryChanges().drop(1).map { },
            backupDao.merchantChanges().drop(1).map { },
            backupDao.savingsGoalChanges().drop(1).map { }
        )

        scope.launch {
            settings.autoBackupFolder
                .map { it != null }
                .distinctUntilChanged()
                .flatMapLatest { enabled ->
                    if (enabled) changes.onStart { emit(Unit) } else emptyFlow()
                }
                .collect { schedule() }
        }
    }

    private fun schedule() {
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(DELAY_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        /** Name of the single pending backup job; scheduling again replaces it. */
        const val WORK_NAME = "auto-backup"

        /** How long after the last change the backup runs. */
        const val DELAY_SECONDS = 20L
    }
}
