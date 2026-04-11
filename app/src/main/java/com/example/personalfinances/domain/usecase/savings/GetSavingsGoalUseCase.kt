package com.example.personalfinances.domain.usecase.savings

import com.example.personalfinances.domain.model.SavingsGoal
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    operator fun invoke(): Flow<SavingsGoal?> = repository.getSavingsGoal()
}
