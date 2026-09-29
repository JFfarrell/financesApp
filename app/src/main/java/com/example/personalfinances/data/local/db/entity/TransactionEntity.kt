package com.example.personalfinances.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.RESTRICT
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName="transactions",
    foreignKeys = [
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchant_id"],
            onDelete = SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = RESTRICT
        )
    ],
    indices = [
        Index("date"),
        Index("transaction_type"),
        Index("category_id"),
        Index("merchant_id")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "transaction_type")val transactionType: String,
    val amount : Double,
    val date: LocalDate,
    @ColumnInfo(name = "cadence_unit")val cadenceUnit: String,
    @ColumnInfo(name = "cadence_value")val cadenceValue: Int,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "merchant_id") val merchantId: String?,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean,
    @ColumnInfo(name = "recurring_group_id") val recurringGroupId: String?,
    val notes: String?,
    val tags: String

)