package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.SavingsGoalEntity
import com.example.personalfinances.domain.model.SavingsGoal

/**
 * Extension functions to convert between [SavingsGoalEntity] (data layer) and [SavingsGoal]
 * (domain layer).
 *
 * Note: [SavingsGoal] no longer stores `currentSaved`. That value is computed at runtime in the
 * ViewModel by combining this goal's [SavingsGoal.startingAmount] with the sum of all
 * SAVINGS-type expenses.
 */

/** Converts a [SavingsGoalEntity] to a domain [SavingsGoal]. */
fun SavingsGoalEntity.toDomain() = SavingsGoal(
    targetAmount = targetAmount,
    startingAmount = startingAmount
)

/** Converts a domain [SavingsGoal] to a [SavingsGoalEntity] for database persistence. */
fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = 1,
    targetAmount = targetAmount,
    startingAmount = startingAmount
)
