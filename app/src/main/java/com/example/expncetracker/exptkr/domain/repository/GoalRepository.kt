package com.example.expncetracker.exptkr.domain.repository

import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<GoalEntity>>
    suspend fun getGoalById(id: Long): GoalEntity?
    suspend fun insertGoal(goal: GoalEntity)
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(goal: GoalEntity)

    // NEW for Step 3
    suspend fun contributeToGoal(goalId: Long, amount: Double)
    suspend fun recalculateGoalProgress(goalId: Long)
    suspend fun recalculateGoalsByCategory(category: String)
}
