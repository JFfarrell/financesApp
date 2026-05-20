package com.example.personalfinances.domain.model

/**
 * Fixed set of income types a user can assign to an [Income] entry.
 *
 * Each entry implements [TransactionType], providing a display name and a default description
 * that is shown read-only in the UI. [OTHER] is the sole exception: its description is blank
 * and editable, allowing the user to describe an income source that doesn't fit the other types.
 *
 * These values are persisted in the database as their [name] string (e.g. "SALARY"),
 * converted back via [IncomeType.valueOf] in the data mapper.
 */
enum class IncomeType(
    override val displayName: String,
    override val defaultDescription: String,
    override val isDescriptionEditable: Boolean = false
) : TransactionType {

    SALARY(
        displayName = "Salary",
        defaultDescription = "Regular monthly employment income"
    ),
    STOCK_RSU(
        displayName = "Stock / RSU",
        defaultDescription = "Vested restricted stock units or stock options"
    ),
    BONUS(
        displayName = "Bonus",
        defaultDescription = "Performance or annual bonus payment"
    ),
    LEFTOVERS(
        displayName = "Leftovers",
        defaultDescription = "Unspent balance carried over from the previous month"
    ),
    OTHER(
        displayName = "Other",
        defaultDescription = "",
        isDescriptionEditable = true
    )
}
