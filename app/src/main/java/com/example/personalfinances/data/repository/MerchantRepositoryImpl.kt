package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.db.dao.MerchantDao
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [MerchantRepository]; converts between [Merchant] and its Room entity. */
class MerchantRepositoryImpl @Inject constructor(
    private val merchantDao: MerchantDao
) : MerchantRepository {

    override fun getAll(): Flow<List<Merchant>> =
        merchantDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(merchantId: String): Flow<Merchant?> =
        merchantDao.getById(merchantId).map { it?.toDomain() }

    override suspend fun add(merchant: Merchant) =
        merchantDao.insertMerchant(merchant.toEntity())
}
