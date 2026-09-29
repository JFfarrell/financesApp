package com.example.personalfinances.domain.usecase.merchant

import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.repository.MerchantRepository
import javax.inject.Inject

/** Saves a new merchant. */
class AddMerchantUseCase @Inject constructor(
    private val repository: MerchantRepository
) {
    suspend operator fun invoke(merchant: Merchant) = repository.add(merchant)
}
