package com.example.personalfinances.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.usecase.backup.RunAutoBackupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The background job that takes an automatic backup. A failure is recorded by the repository
 * (and shown in Settings) rather than retried here: the next data change, or the next app start,
 * schedules another attempt, and a permission problem would only fail again.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val runAutoBackup: RunAutoBackupUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (runAutoBackup()) {
        is BackupResult.Success -> Result.success()
        is BackupResult.Failure -> Result.failure()
    }
}
