package com.example.personalfinances.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a single income record in the "incomes" table.
 *
 * [type] stores the name of the [com.example.personalfinances.domain.model.IncomeType] enum
 * entry as a plain string (e.g. "SALARY"). This avoids needing a TypeConverter and keeps the
 * mapping logic explicit in [com.example.personalfinances.data.mapper.IncomeMapper].
 *
 * [description] is only populated when [type] is "OTHER". For all other types the description
 * is derived from the enum's [defaultDescription] at read time and not stored in the database.
 *
 * [isRecurring] and [cadenceMonths] mirror the same fields on [com.example.personalfinances.data.local.db.entity.ExpenseEntity].
 * [recurringGroupId] is a UUID string shared by all entries in the same recurring series; null
 * for one-off records.
 */
@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val description: String?,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean,
    @ColumnInfo(name = "cadence_months") val cadenceMonths: Int,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "recurring_group_id") val recurringGroupId: String? = null
)
