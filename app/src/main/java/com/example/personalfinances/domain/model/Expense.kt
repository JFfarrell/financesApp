package com.example.personalfinances.domain.model

/**
 * Domain model representing a single expense record.
 *
 * [type] places this expense within the two-level hierarchy of [ExpenseCategory] → [ExpenseType].
 * The mapper converts to/from the raw string stored in the database.
 *
 * [title] is the required short user-provided label for this entry (e.g. "Tesco", "Monthly gym").
 *
 * [description] is an optional note; required only when [ExpenseType.isDescriptionEditable] is
 * true (i.e. `*_OTHER` variants).
 *
 * [cadenceMonths] of 0 means a one-time expense; any positive value is a recurring interval.
 */
data class Expense(
    val id: Long = 0,
    val amount: Double,
    val title: String,
    val description: String,
    val type: ExpenseType,
    val date: Long,
    val isRecurring: Boolean,
    val cadenceMonths: Int
)
