package com.example.personalfinances.domain.model

import com.example.personalfinances.domain.model.enums.TransactionType

/**
 * A user-defined category. Each category belongs to one [TransactionType], so expense, income
 * and savings transactions each pick from their own list.
 */
data class Category(
    val id: String,
    val name: String,
    val type: TransactionType
)
