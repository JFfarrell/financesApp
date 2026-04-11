package com.example.personalfinances.domain.model

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val title: String,
    val description: String,
    val categoryId: Long?,
    val date: Long,
    val isRecurring: Boolean,
    val cadenceMonths: Int
)
