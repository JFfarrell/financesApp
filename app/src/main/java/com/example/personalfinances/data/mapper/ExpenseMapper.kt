package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.ExpenseEntity
import com.example.personalfinances.domain.model.Expense
import com.example.personalfinances.domain.model.ExpenseType

/**
 * Extension functions to convert between [ExpenseEntity] (data layer) and [Expense] (domain layer).
 *
 * [ExpenseType] is stored as its [Enum.name] string and parsed back with [ExpenseType.valueOf].
 * An unrecognised type string throws [IllegalArgumentException] intentionally — data integrity
 * problems should surface immediately.
 */

/** Converts a database [ExpenseEntity] to a domain [Expense] model. */
fun ExpenseEntity.toDomain() = Expense(
    id = id,
    amount = amount,
    title = title,
    description = description,
    type = ExpenseType.valueOf(type),
    date = date,
    isRecurring = isRecurring,
    cadenceMonths = cadenceMonths,
    recurringGroupId = recurringGroupId
)

/** Converts a domain [Expense] to an [ExpenseEntity] ready for database insertion. */
fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amount = amount,
    title = title,
    description = description,
    type = type.name,
    date = date,
    isRecurring = isRecurring,
    cadenceMonths = cadenceMonths,
    recurringGroupId = recurringGroupId
)
