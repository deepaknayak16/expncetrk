package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.*
import com.example.expncetracker.exptkr.data.db.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM classification_rules WHERE isActive = 1 ORDER BY priority DESC")
    fun getActiveRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM classification_rules WHERE isActive = 1 ORDER BY priority DESC")
    suspend fun getActiveRulesList(): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RuleEntity>)

    @Query("DELETE FROM classification_rules")
    suspend fun deleteAllRules()
}
