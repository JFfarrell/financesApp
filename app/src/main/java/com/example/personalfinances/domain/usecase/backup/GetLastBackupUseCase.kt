package com.example.personalfinances.domain.usecase.backup

import com.example.personalfinances.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits when the last backup was saved (epoch milliseconds), or null if never. */
class GetLastBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    operator fun invoke(): Flow<Long?> = repository.getLastBackupAt()
}
