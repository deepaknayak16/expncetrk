package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.data.db.entity.MerchantMappingEntity
import com.example.expncetracker.exptkr.domain.repository.MerchantMappingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantMappingRepositoryImpl @Inject constructor(
    private val merchantMappingDao: MerchantMappingDao
) : MerchantMappingRepository {
    override suspend fun insertMapping(mapping: MerchantMappingEntity) = merchantMappingDao.insertMapping(mapping)
    override suspend fun getMappingForMerchant(merchantName: String): MerchantMappingEntity? = merchantMappingDao.getMappingForMerchant(merchantName)
    override fun getAllMappings(): Flow<List<MerchantMappingEntity>> = merchantMappingDao.getAllMappings()
}
