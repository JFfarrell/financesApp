package com.example.personalfinances.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.personalfinances.domain.model.enums.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists app-wide user preferences to the shared DataStore Preferences instance.
 *
 * Stores [payCycleStartDay] — the day of month (1–28) on which each financial period begins,
 * defaulting to 1 (calendar-month behaviour) — and [themeMode], defaulting to following the
 * system setting.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val PAY_CYCLE_START_DAY_KEY = intPreferencesKey("pay_cycle_start_day")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LAST_BACKUP_AT_KEY = longPreferencesKey("last_backup_at")
    }

    val payCycleStartDay: Flow<Int> = dataStore.data.map { it[PAY_CYCLE_START_DAY_KEY] ?: 1 }

    suspend fun savePayCycleStartDay(day: Int) {
        dataStore.edit { it[PAY_CYCLE_START_DAY_KEY] = day }
    }

    /** Emits the saved theme mode; an unknown or missing value means [ThemeMode.SYSTEM]. */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE_KEY] } ?: ThemeMode.SYSTEM
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    /** Emits when the last backup was saved (epoch milliseconds), or null if there never was one. */
    val lastBackupAt: Flow<Long?> = dataStore.data.map { it[LAST_BACKUP_AT_KEY] }

    suspend fun saveLastBackupAt(epochMillis: Long) {
        dataStore.edit { it[LAST_BACKUP_AT_KEY] = epochMillis }
    }
}
