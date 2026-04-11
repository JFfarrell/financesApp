package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.IncomeEntity
import com.example.personalfinances.domain.model.Income

fun IncomeEntity.toDomain() = Income(
    id = id,
    amount = amount,
    source = source,
    cadenceMonths = cadenceMonths,
    startDate = startDate
)

fun Income.toEntity() = IncomeEntity(
    id = id,
    amount = amount,
    source = source,
    cadenceMonths = cadenceMonths,
    startDate = startDate
)
