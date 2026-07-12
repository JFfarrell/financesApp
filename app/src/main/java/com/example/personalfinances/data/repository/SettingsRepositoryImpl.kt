package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.datastore.SettingsDataStore
import com.example.personalfinances.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * DataStore-backed implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override fun getPayCycleStartDay(): Flow<Int> = dataStore.payCycleStartDay
    override suspend fun savePayCycleStartDay(day: Int) = dataStore.savePayCycleStartDay(day)
}
