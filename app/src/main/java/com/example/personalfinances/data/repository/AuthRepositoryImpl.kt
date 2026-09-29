package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.datastore.AuthDataStore
import com.example.personalfinances.domain.repository.AuthRepository
import com.example.personalfinances.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * DataStore-backed [AuthRepository]. Hashing is deliberately slow (see [PasswordHasher]), so it
 * runs on a background dispatcher and never blocks the main thread.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authDataStore: AuthDataStore
) : AuthRepository {

    override fun isPasswordSet(): Flow<Boolean> =
        authDataStore.passwordHash.map { !it.isNullOrEmpty() }

    override suspend fun setPassword(password: String) {
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
        authDataStore.savePasswordHash(hash)
    }

    /**
     * Checks [password] against the stored hash. When it matches but the stored hash is in the
     * old unsalted format (or uses fewer iterations than now), it is silently replaced with a
     * current one, so existing users are upgraded without having to reset their password.
     */
    override suspend fun verifyPassword(password: String): Boolean {
        val stored = authDataStore.passwordHash.first()?.takeIf { it.isNotEmpty() } ?: return false
        val matches = withContext(Dispatchers.Default) { PasswordHasher.verify(password, stored) }
        if (matches && PasswordHasher.needsUpgrade(stored)) {
            val upgraded = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
            authDataStore.savePasswordHash(upgraded)
        }
        return matches
    }
}
