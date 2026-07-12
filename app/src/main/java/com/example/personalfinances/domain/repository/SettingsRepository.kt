package com.example.personalfinances.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for app-wide user preferences.
 *
 * [getPayCycleStartDay] emits the day of the month (1–28) on which each financial period begins.
 * Emits 1 by default (calendar-month behaviour).
 */
interface SettingsRepository {
    fun getPayCycleStartDay(): Flow<Int>
    suspend fun savePayCycleStartDay(day: Int)
}
