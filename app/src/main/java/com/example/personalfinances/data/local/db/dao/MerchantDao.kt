package com.example.personalfinances.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalfinances.data.local.db.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants")
    fun getAll() : Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE id = :merchantId")
    fun getById(merchantId: String) : Flow<MerchantEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity)

    @Update
    suspend fun updateMerchant(merchant: MerchantEntity)

    @Delete
    suspend fun deleteMerchant(merchant: MerchantEntity)
}