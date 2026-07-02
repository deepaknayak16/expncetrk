package com.example.expncetracker.exptkr.data.repository

import com.example.expncetracker.exptkr.data.db.dao.RuleDao
import com.example.expncetracker.exptkr.data.db.entity.RuleEntity
import com.example.expncetracker.exptkr.domain.model.ClassificationRule
import com.example.expncetracker.exptkr.domain.model.MatchType
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao
) : RuleRepository {
    override fun getActiveRules(): Flow<List<ClassificationRule>> = 
        ruleDao.getActiveRules().map { entities -> 
            entities.map { it.toDomain() } 
        }

    override suspend fun getActiveRulesList(): List<ClassificationRule> = 
        ruleDao.getActiveRulesList().map { it.toDomain() }

    override suspend fun insertRules(rules: List<ClassificationRule>) = 
        ruleDao.insertRules(rules.map { it.toEntity() })

    override suspend fun deleteAllRules() = ruleDao.deleteAllRules()

    override suspend fun deleteSystemRules() = ruleDao.deleteSystemRules()

    private fun RuleEntity.toDomain() = ClassificationRule(
        keyword = keyword,
        category = category,
        matchType = MatchType.valueOf(matchType),
        priority = priority,
        transactionType = transactionType,
        isSystemRule = isSystemRule
    )

    private fun ClassificationRule.toEntity() = RuleEntity(
        keyword = keyword,
        category = category,
        matchType = matchType.name,
        priority = priority,
        transactionType = transactionType,
        isActive = true,
        isSystemRule = isSystemRule
    )
}
