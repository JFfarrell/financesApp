package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.model.AutoBackupStatus
import com.example.personalfinances.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits whether automatic backup is on, which folder it uses, and the last error if any. */
class GetAutoBackupStatusUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    operator fun invoke(): Flow<AutoBackupStatus> = repository.getAutoBackupStatus()
}
