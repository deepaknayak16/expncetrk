package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.domain.model.ClassificationRule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getActiveRules(): Flow<List<ClassificationRule>>
    suspend fun getActiveRulesList(): List<ClassificationRule>
    suspend fun insertRules(rules: List<ClassificationRule>)
    suspend fun deleteAllRules()
    suspend fun deleteSystemRules()
}
