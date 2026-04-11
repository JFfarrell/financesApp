package com.example.personalfinances.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isPasswordSet(): Flow<Boolean>
    suspend fun setPassword(password: String)
    suspend fun verifyPassword(password: String): Boolean
}
