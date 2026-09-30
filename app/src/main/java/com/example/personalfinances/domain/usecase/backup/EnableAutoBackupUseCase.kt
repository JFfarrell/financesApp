package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.model.BackupResult
import com.example.personalfinances.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Turns automatic backup on, saving to the folder at [folder] (an address from the system folder
 * picker), and takes a first backup straight away so the user can see it working.
 */
class EnableAutoBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(folder: String): BackupResult = repository.enableAutoBackup(folder)
}
