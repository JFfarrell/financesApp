package com.example.personalfinances.data.mapper

import com.example.personalfinances.data.local.db.entity.MerchantEntity
import com.example.personalfinances.domain.model.Merchant

fun MerchantEntity.toDomain() = Merchant(
    id = id,
    name = name
)

fun Merchant.toEntity() = MerchantEntity(
    id = id,
    name = name
)
