package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.repository.BackupRepository
import javax.inject.Inject

/** Turns automatic backup off. Backup files already written are kept. */
class DisableAutoBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke() = repository.disableAutoBackup()
}
