package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.IncomeEntity
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.model.IncomeType

/**
 * Extension functions to convert between the Room [IncomeEntity] (data layer) and the
 * [Income] domain model (domain layer).
 *
 * The [IncomeType] enum is stored in the database as its [Enum.name] string (e.g. "SALARY").
 * [IncomeType.valueOf] is used on the way back out. If the database somehow contains an
 * unrecognised type string, [valueOf] will throw an [IllegalArgumentException], which is
 * intentional — it surfaces data integrity problems immediately rather than hiding them.
 */

/** Converts a database [IncomeEntity] to a domain [Income] model. */
fun IncomeEntity.toDomain() = Income(
    id = id,
    amount = amount,
    type = IncomeType.valueOf(type),
    description = description,
    cadenceMonths = cadenceMonths,
    startDate = startDate
)

/** Converts a domain [Income] model to a [IncomeEntity] ready for database insertion. */
fun Income.toEntity() = IncomeEntity(
    id = id,
    amount = amount,
    type = type.name,
    description = description,
    cadenceMonths = cadenceMonths,
    startDate = startDate
)
