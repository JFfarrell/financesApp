package com.example.personalfinances.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val PASSWORD_HASH_KEY = stringPreferencesKey("password_hash")
    }

    val passwordHash: Flow<String?> = dataStore.data.map { it[PASSWORD_HASH_KEY] }

    suspend fun savePasswordHash(hash: String) {
        dataStore.edit { it[PASSWORD_HASH_KEY] = hash }
    }
}
