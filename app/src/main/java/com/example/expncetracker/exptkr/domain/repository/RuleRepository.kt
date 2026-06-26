package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.data.db.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getActiveRules(): Flow<List<RuleEntity>>
    suspend fun getActiveRulesList(): List<RuleEntity>
    suspend fun insertRules(rules: List<RuleEntity>)
    suspend fun deleteAllRules()
}
