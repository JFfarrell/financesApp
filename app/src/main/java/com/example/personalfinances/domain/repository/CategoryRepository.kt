package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAll(): Flow<List<Category>>
    suspend fun add(category: Category)
    fun getById(categoryId: String): Flow<Category?>

    /** Emits, for each category id that has transactions, how many transactions use it. */
    fun getUsageCounts(): Flow<Map<String, Int>>

    /** Saves changes to an existing category (for example a new name). */
    suspend fun update(category: Category)

    /** Removes a category. The database refuses if any transaction still uses it. */
    suspend fun delete(category: Category)
}
