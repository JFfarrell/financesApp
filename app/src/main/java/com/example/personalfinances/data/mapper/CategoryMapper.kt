package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.enums.TransactionType

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    type = TransactionType.valueOf(transactionType)
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    transactionType = type.name
)
