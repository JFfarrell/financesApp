package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getSavingsGoal(): Flow<SavingsGoal?>
    suspend fun upsertSavingsGoal(goal: SavingsGoal)
}
