package com.example.personalfinances.domain.model

/**
 * Shared contract for predefined transaction type enums (e.g. [IncomeType], and a future ExpenseType).
 *
 * Each enum that implements this interface represents a fixed set of named types a user can
 * assign to a transaction. Most types carry a read-only [defaultDescription] that explains what
 * the type means. The special "Other" entry in each enum sets [isDescriptionEditable] to true,
 * allowing the user to provide their own description at entry time.
 */
interface TransactionType {

    /** Human-readable label shown in dropdowns and list items (e.g. "Stock / RSU"). */
    val displayName: String

    /**
     * Pre-written description shown below the type selector.
     * Empty string for the "Other" type, where the user supplies their own text.
     */
    val defaultDescription: String

    /**
     * Whether the description field should be an editable text input.
     * True only for the "Other" entry; false for all fixed types.
     */
    val isDescriptionEditable: Boolean
}
