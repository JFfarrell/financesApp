package com.example.personalfinances.domain.usecase.auth

import com.example.personalfinances.domain.repository.AuthRepository
import javax.inject.Inject

class SetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(password: String) = repository.setPassword(password)
}
