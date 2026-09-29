package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    suspend fun add(category: Category)
    fun getById(categoryId: String): Flow<Category?>
}