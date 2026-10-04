package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.TransactionEntity
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.Transaction
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

fun TransactionEntity.toDomain(category: Category, merchant: Merchant?) = Transaction(
    id = id,
    transactionType = TransactionType.valueOf(transactionType),
    amount = amount,
    date = date,
    cadenceUnit = CadenceUnit.valueOf(cadenceUnit),
    cadenceValue = cadenceValue,
    category = category,
    merchant = merchant,
    isRecurring = isRecurring,
    recurringGroupId = recurringGroupId,
    notes = notes,
    tags = Json.decodeFromString<Set<String>>(tags)
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    transactionType = transactionType.name,
    amount = amount,
    date = date,
    cadenceUnit = cadenceUnit.name,
    cadenceValue = cadenceValue,
    categoryId = category.id,
    merchantId = merchant?.id,
    isRecurring = isRecurring,
    recurringGroupId = recurringGroupId,
    notes = notes,
    tags = Json.encodeToString(tags)
)