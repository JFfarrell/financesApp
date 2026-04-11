package com.example.personalfinances.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalfinances.data.local.db.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes ORDER BY start_date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Query("""
        SELECT * FROM incomes
        WHERE start_date >= :monthStart AND start_date < :monthEnd
        ORDER BY start_date DESC
    """)
    fun getIncomesByMonth(monthStart: Long, monthEnd: Long): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)
}
