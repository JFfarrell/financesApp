package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.repository.MerchantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits every merchant, updating whenever one is added. */
class GetMerchantsUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    operator fun invoke(): Flow<List<Merchant>> = repository.getAll()
}
