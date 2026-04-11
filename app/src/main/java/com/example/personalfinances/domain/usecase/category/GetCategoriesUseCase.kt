package com.example.personalfinances.domain.usecase.category

import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> = repository.getAllCategories()
}
