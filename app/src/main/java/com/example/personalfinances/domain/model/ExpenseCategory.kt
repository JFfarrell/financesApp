package com.example.personalfinances.domain.model

/**
 * Top-level groupings for expense types.
 *
 * Each entry in [ExpenseType] carries a reference back to one of these categories, forming a
 * two-level hierarchy: category → subtype. The UI presents this as two sequential dropdowns —
 * the user picks a category first, then a subtype from that category's entries.
 *
 * [displayName] is the label shown in the category dropdown.
 */
enum class ExpenseCategory(val displayName: String) {
    HOUSING("Housing"),
    HOUSEHOLD("Household"),
    TRANSPORT("Transport"),
    LIFESTYLE("Lifestyle"),
    SAVINGS("Savings")
}
