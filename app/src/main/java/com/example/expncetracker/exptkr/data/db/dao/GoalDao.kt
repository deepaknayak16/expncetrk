package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.*
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, deadline ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE linked_category = :category")
    suspend fun getGoalsByCategory(category: String): List<GoalEntity>

    @Query("""
        UPDATE goals
        SET currentAmount = :spent,
        isCompleted = :spent >= targetAmount
        WHERE id = :goalId
    """)
    suspend fun updateGoalProgress(goalId: Long, spent: java.math.BigDecimal)

    @Query("""
        SELECT amount
        FROM transactions
        WHERE isRecurring = 0 AND category = :category
        AND type = :type
        AND timestamp >= :sinceMillis
    """)
    suspend fun getAmountsByCategorySince(category: String, type: String, sinceMillis: Long): List<java.math.BigDecimal>

    @Transaction
    suspend fun recalculateGoalProgress(goalId: Long, category: String, sinceMillis: Long = 0) {
        val amounts = getAmountsByCategorySince(category, "DEBIT", sinceMillis)
        val spent = amounts.fold(java.math.BigDecimal.ZERO) { acc, amt -> acc.add(amt) }
        updateGoalProgress(goalId, spent)
    }

    @Query("""
        SELECT amount
        FROM transactions
        WHERE category = :category
        AND type = :type
    """)
    suspend fun getAmountsByCategory(category: String, type: String): List<java.math.BigDecimal>
}
