package com.example.personalfinances.domain.model

/**
 * Domain model representing the user's savings goal.
 *
 * [targetAmount] is the total amount the user wants to save.
 *
 * [startingAmount] is a one-time seed value representing savings that existed before the user
 * started tracking in this app. It is the only stored value — the actual "current saved" total
 * is computed in the ViewModel as: startingAmount + sum of all SAVINGS-type expenses.
 *
 * [progressFraction] is a convenience property for the progress arc UI component. It requires
 * the computed [currentSaved] to be passed in, since it is not stored on this model.
 */
data class SavingsGoal(
    val targetAmount: Double,
    val startingAmount: Double
) {
    /**
     * Returns a progress fraction between 0.0 and 1.0 based on [currentSaved] vs [targetAmount].
     *
     * [currentSaved] should be passed as: startingAmount + sum of all SAVINGS expenses.
     */
    fun progressFraction(currentSaved: Double): Float =
        if (targetAmount <= 0.0) 0f
        else (currentSaved / targetAmount).toFloat().coerceIn(0f, 1f)
}
