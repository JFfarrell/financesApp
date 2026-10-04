package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.repository.BackupRepository
import javax.inject.Inject

/** Saves a backup of all data to the file at [destination]. */
class ExportBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(destination: String): BackupResult = repository.exportTo(destination)
}
