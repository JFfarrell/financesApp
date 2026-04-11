package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.datastore.AuthDataStore
import com.example.personalfinances.domain.repository.AuthRepository
import com.example.personalfinances.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authDataStore: AuthDataStore
) : AuthRepository {

    override fun isPasswordSet(): Flow<Boolean> =
        authDataStore.passwordHash.map { !it.isNullOrEmpty() }

    override suspend fun setPassword(password: String) {
        authDataStore.savePasswordHash(PasswordHasher.hash(password))
    }

    override suspend fun verifyPassword(password: String): Boolean {
        val stored = authDataStore.passwordHash.first()
        return stored != null && stored == PasswordHasher.hash(password)
    }
}
