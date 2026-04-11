package com.example.personalfinances.domain.model

data class Income(
    val id: Long = 0,
    val amount: Double,
    val source: String,
    val cadenceMonths: Int,
    val startDate: Long
)
