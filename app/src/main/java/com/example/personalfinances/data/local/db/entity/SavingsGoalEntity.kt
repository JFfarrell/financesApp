package com.example.personalfinances.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the user's savings goal in the "savings_goals" table.
 *
 * This is a singleton record — only one row ever exists, fixed at [id] = 1.
 *
 * [startingAmount] is a user-set seed representing savings that existed before the user began
 * tracking in this app. The actual "current saved" total is NOT stored here — it is computed
 * at runtime as: startingAmount + sum of all SAVINGS-type expenses.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "target_amount") val targetAmount: Double,
    @ColumnInfo(name = "starting_amount") val startingAmount: Double
)
