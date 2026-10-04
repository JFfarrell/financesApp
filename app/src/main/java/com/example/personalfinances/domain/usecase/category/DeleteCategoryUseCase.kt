package com.example.personalfinances.domain.usecase.category

import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Deletes a category, but only if no transaction uses it. A category in use cannot be removed
 * without orphaning those transactions, so the user is told how many there are and can rename the
 * category instead, or change those transactions first.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): OperationResult {
        val used = repository.getUsageCounts().first()[category.id] ?: 0
        if (used > 0) {
            val noun = if (used == 1) "transaction" else "transactions"
            return OperationResult.Failure(
                "\"${category.name}\" is used by $used $noun. Rename it instead, or move those " +
                    "transactions to another category first."
            )
        }
        return try {
            repository.delete(category)
            OperationResult.Success
        } catch (e: Exception) {
            OperationResult.Failure("Could not delete \"${category.name}\".")
        }
    }
}
