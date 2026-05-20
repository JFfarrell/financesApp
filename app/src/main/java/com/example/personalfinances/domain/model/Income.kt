package com.example.personalfinances.domain.model

/**
 * Domain model representing a single income record.
 *
 * [type] is a typed [IncomeType] enum value — the mapper is responsible for converting to/from
 * the raw string stored in the database.
 *
 * [description] holds user-provided text only when [type] is [IncomeType.OTHER]. For all other
 * types it is null; the display description is read directly from [IncomeType.defaultDescription].
 *
 * [cadenceMonths] of 0 means a one-time payment; any positive value is a recurring interval.
 */
data class Income(
    val id: Long = 0,
    val amount: Double,
    val type: IncomeType,
    val description: String?,
    val cadenceMonths: Int,
    val startDate: Long
)
