package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.enums.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository for app-wide user preferences.
 *
 * [getPayCycleStartDay] emits the day of the month (1–28) on which each financial period begins.
 * Emits 1 by default (calendar-month behaviour).
 *
 * [getThemeMode] emits how the app chooses light or dark; [ThemeMode.SYSTEM] by default.
 */
interface SettingsRepository {
    fun getPayCycleStartDay(): Flow<Int>
    suspend fun savePayCycleStartDay(day: Int)
    fun getThemeMode(): Flow<ThemeMode>

    /** Emits the chosen ISO 4217 currency code, or null to follow the phone's currency. */
    fun getCurrencyCode(): Flow<String?>
    suspend fun saveCurrencyCode(code: String?)
    suspend fun saveThemeMode(mode: ThemeMode)
}
