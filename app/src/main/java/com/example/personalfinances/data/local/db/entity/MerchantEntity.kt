package com.example.personalfinances.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val name: String
)