package com.example.personalfinances.domain.usecase.category

import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Renames a category. Every transaction using it shows the new name straight away, because
 * transactions refer to the category by id.
 *
 * Refused if the name is blank, or if another category of the same type already has that name
 * (ignoring case), which would leave two indistinguishable choices in the picker.
 */
class RenameCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category, newName: String): OperationResult {
        val name = newName.trim()
        if (name.isEmpty()) return OperationResult.Failure("The name can't be empty.")
        if (name == category.name) return OperationResult.Success

        val clash = repository.getAll().first().any {
            it.id != category.id && it.type == category.type && it.name.equals(name, ignoreCase = true)
        }
        if (clash) {
            return OperationResult.Failure(
                "There is already a ${category.type.displayName.lowercase()} category called \"$name\"."
            )
        }
        repository.update(category.copy(name = name))
        return OperationResult.Success
    }
}
