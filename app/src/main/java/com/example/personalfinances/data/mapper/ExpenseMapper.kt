package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.ExpenseEntity
import com.example.personalfinances.domain.model.Expense

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    amount = amount,
    title = title,
    description = description,
    categoryId = categoryId,
    date = date,
    isRecurring = isRecurring,
    cadenceMonths = cadenceMonths
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amount = amount,
    title = title,
    description = description,
    categoryId = categoryId,
    date = date,
    isRecurring = isRecurring,
    cadenceMonths = cadenceMonths
)
