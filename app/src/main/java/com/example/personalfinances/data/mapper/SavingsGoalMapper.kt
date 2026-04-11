package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.SavingsGoalEntity
import com.example.personalfinances.domain.model.SavingsGoal

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    targetAmount = targetAmount,
    currentSaved = currentSaved
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = 1,
    targetAmount = targetAmount,
    currentSaved = currentSaved
)
