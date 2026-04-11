package com.example.personalfinances.domain.usecase.savings

import com.example.personalfinances.domain.model.SavingsGoal
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import javax.inject.Inject

class UpdateSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.upsertSavingsGoal(goal)
}
