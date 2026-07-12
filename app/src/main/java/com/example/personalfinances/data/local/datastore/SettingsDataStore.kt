package com.example.personalfinances.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists app-wide user preferences to the shared DataStore Preferences instance.
 *
 * Currently stores [payCycleStartDay] — the day of month (1–28) on which each financial period
 * begins. Defaults to 1 (calendar-month behaviour) if never set.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val PAY_CYCLE_START_DAY_KEY = intPreferencesKey("pay_cycle_start_day")
    }

    val payCycleStartDay: Flow<Int> = dataStore.data.map { it[PAY_CYCLE_START_DAY_KEY] ?: 1 }

    suspend fun savePayCycleStartDay(day: Int) {
        dataStore.edit { it[PAY_CYCLE_START_DAY_KEY] = day }
    }
}
