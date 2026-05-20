package com.example.personalfinances.domain.repository

import com.example.personalfinances.domain.model.Income
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    fun getAllIncomes(): Flow<List<Income>>
    fun getIncomesByMonth(monthStart: Long, monthEnd: Long): Flow<List<Income>>
    suspend fun addIncome(income: Income)
    suspend fun deleteIncome(income: Income)
    suspend fun updateIncome(income: Income)
    suspend fun deleteIncomeSeries(groupId: String, fromDate: Long)
    suspend fun updateIncomeSeries(income: Income)
}
