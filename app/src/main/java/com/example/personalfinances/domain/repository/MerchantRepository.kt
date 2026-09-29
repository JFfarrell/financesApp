package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Merchant
import kotlinx.coroutines.flow.Flow

interface MerchantRepository {
    fun getAll(): Flow<List<Merchant>>
    fun getById(merchantId: String): Flow<Merchant?>
    suspend fun add(merchant: Merchant)
}
