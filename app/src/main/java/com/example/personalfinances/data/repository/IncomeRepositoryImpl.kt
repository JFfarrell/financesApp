package com.example.personalfinances.data.repository

import com.example.personalfinances.data.local.db.dao.IncomeDao
import com.example.personalfinances.data.mapper.toDomain
import com.example.personalfinances.data.mapper.toEntity
import com.example.personalfinances.domain.model.Income
import com.example.personalfinances.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IncomeRepositoryImpl @Inject constructor(
    private val dao: IncomeDao
) : IncomeRepository {

    override fun getAllIncomes(): Flow<List<Income>> =
        dao.getAllIncomes().map { list -> list.map { it.toDomain() } }

    override fun getIncomesByMonth(monthStart: Long, monthEnd: Long): Flow<List<Income>> =
        dao.getIncomesByMonth(monthStart, monthEnd).map { list -> list.map { it.toDomain() } }

    override suspend fun addIncome(income: Income) {
        dao.insertIncome(income.toEntity())
    }

    override suspend fun deleteIncome(income: Income) {
        dao.deleteIncome(income.toEntity())
    }

    override suspend fun updateIncome(income: Income) {
        dao.updateIncome(income.toEntity())
    }
}
