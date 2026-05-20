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
 * [isRecurring] distinguishes a recurring payment from a one-off. When false, [cadenceMonths]
 * is 0 and no series operations apply. When true, [cadenceMonths] holds the repeat interval and
 * [recurringGroupId] links all entries in the same series.
 */
data class Income(
    val id: Long = 0,
    val amount: Double,
    val type: IncomeType,
    val description: String?,
    val isRecurring: Boolean,
    val cadenceMonths: Int,
    val startDate: Long,
    val recurringGroupId: String? = null
)
