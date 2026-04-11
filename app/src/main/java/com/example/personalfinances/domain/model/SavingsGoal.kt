package com.example.personalfinances.domain.model

data class SavingsGoal(
    val targetAmount: Double,
    val currentSaved: Double
) {
    val progressFraction: Float
        get() = if (targetAmount <= 0.0) 0f
                else (currentSaved / targetAmount).toFloat().coerceIn(0f, 1f)
}
