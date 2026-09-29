package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.db.dao.CategoryDao
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [CategoryRepository]; converts between [Category] and its Room entity. */
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAll(): Flow<List<Category>> =
        categoryDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getById(categoryId: String): Flow<Category?> =
        categoryDao.getById(categoryId).map { it?.toDomain() }

    override suspend fun add(category: Category) =
        categoryDao.insertCategory(category.toEntity())
}
