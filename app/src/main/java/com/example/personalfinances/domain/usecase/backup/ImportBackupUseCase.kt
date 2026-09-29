package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.repository.BackupRepository
import javax.inject.Inject

/** Merges the backup file at [source] into the app's data. Nothing is deleted. */
class ImportBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(source: String): BackupResult = repository.importFrom(source)
}
