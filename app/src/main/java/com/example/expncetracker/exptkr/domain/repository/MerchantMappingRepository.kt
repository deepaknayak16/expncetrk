package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.data.db.entity.MerchantMappingEntity
import kotlinx.coroutines.flow.Flow

interface MerchantMappingRepository {
    suspend fun insertMapping(mapping: MerchantMappingEntity)
    suspend fun getMappingForMerchant(merchantName: String): MerchantMappingEntity?
    fun getAllMappings(): Flow<List<MerchantMappingEntity>>
}
