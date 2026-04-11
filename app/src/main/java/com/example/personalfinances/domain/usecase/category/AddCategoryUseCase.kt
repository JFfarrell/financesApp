package com.example.personalfinances.domain.usecase.category

import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) = repository.addCategory(category)
}
