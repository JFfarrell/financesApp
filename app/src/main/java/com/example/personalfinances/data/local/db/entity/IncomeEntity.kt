package com.example.personalfinances.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val source: String,
    @ColumnInfo(name = "cadence_months") val cadenceMonths: Int,
    @ColumnInfo(name = "start_date") val startDate: Long
)
