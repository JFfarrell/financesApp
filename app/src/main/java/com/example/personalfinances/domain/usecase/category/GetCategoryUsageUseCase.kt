package com.example.personalfinances.domain.usecase.category

import com.example.personalfinances.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Emits how many transactions use each category (by category id), updating as they change. */
class GetCategoryUsageUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<Map<String, Int>> = repository.getUsageCounts()
}
