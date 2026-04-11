package com.example.personalfinances.domain.usecase.auth

import com.example.personalfinances.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsPasswordSetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isPasswordSet()
}
