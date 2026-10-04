package com.example.personalfinances.domain.model

import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import java.time.LocalDate

data class Transaction(
    val id: String,
    val transactionType: TransactionType,
    val amount: Double,
    val date: LocalDate,
    val cadenceUnit: CadenceUnit,
    val cadenceValue: Int,
    val category: Category,
    val merchant: Merchant?,
    val isRecurring: Boolean,
    val recurringGroupId: String?,
    val notes: String?,
    val tags: Set<String>
)
