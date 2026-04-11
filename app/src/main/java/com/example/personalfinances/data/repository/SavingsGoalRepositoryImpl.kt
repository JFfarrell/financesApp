package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.db.dao.SavingsGoalDao
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.SavingsGoal
import com.example.personalfinances.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavingsGoalRepositoryImpl @Inject constructor(
    private val dao: SavingsGoalDao
) : SavingsGoalRepository {

    override fun getSavingsGoal(): Flow<SavingsGoal?> =
        dao.getSavingsGoal().map { it?.toDomain() }

    override suspend fun upsertSavingsGoal(goal: SavingsGoal) {
        dao.upsertSavingsGoal(goal.toEntity())
    }
}
