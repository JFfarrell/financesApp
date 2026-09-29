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
    suspend fun saveThemeMode(mode: ThemeMode)
}
