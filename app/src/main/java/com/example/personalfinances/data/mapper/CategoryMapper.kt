package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.domain.model.Category

fun CategoryEntity.toDomain() = Category(id = id, name = name)
fun Category.toEntity() = CategoryEntity(id = id, name = name)
