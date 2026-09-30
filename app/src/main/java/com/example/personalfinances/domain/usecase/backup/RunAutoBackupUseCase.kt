package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.repository.BackupRepository
import javax.inject.Inject

/** Takes one automatic backup to the chosen folder. Run by the background worker after data changes. */
class RunAutoBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(): BackupResult = repository.runAutoBackup()
}
