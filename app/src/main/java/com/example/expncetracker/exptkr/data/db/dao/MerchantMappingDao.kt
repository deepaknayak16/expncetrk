package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.*
import com.example.expncetracker.exptkr.data.db.entity.MerchantMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantMappingDao {
    @Query("SELECT * FROM merchant_mappings")
    fun getAllMappings(): Flow<List<MerchantMappingEntity>>

    @Query("SELECT * FROM merchant_mappings WHERE merchantName = :merchantName COLLATE NOCASE")
    suspend fun getMappingForMerchant(merchantName: String): MerchantMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantMappingEntity)

    @Delete
    suspend fun deleteMapping(mapping: MerchantMappingEntity)
}
