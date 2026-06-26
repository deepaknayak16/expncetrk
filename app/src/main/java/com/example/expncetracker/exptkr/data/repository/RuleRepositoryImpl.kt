package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.RuleDao
import com.example.expncetracker.exptkr.data.db.entity.RuleEntity
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao
) : RuleRepository {
    override fun getActiveRules(): Flow<List<RuleEntity>> = ruleDao.getActiveRules()
    override suspend fun getActiveRulesList(): List<RuleEntity> = ruleDao.getActiveRulesList()
    override suspend fun insertRules(rules: List<RuleEntity>) = ruleDao.insertRules(rules)
    override suspend fun deleteAllRules() = ruleDao.deleteAllRules()
}
