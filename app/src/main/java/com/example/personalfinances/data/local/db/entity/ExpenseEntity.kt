package com.example.personalfinances.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single expense record in the "expenses" table.
 *
 * [type] stores the [com.example.personalfinances.domain.model.ExpenseType] enum name
 * (e.g. `"TRANSPORT_FUEL"`). Conversion is handled in
 * [com.example.personalfinances.data.mapper.ExpenseMapper].
 *
 * [title] is the required short user-provided label for this entry (e.g. "Tesco", "Monthly gym").
 *
 * [description] is an optional note; required only for `*_OTHER` type variants.
 */
@Entity(
    tableName = "expenses",
    indices = [Index("type"), Index("date")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val title: String,
    val description: String,
    val type: String,
    val date: Long,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean,
    @ColumnInfo(name = "cadence_months") val cadenceMonths: Int,
    @ColumnInfo(name = "recurring_group_id") val recurringGroupId: String? = null
)
