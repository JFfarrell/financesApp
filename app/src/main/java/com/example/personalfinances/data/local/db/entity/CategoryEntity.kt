package com.example.personalfinances.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "transaction_type") val transactionType: String
)
