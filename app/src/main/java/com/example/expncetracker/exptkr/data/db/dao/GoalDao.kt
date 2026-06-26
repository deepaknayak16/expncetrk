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
        SELECT COALESCE(SUM(amount), '0.0')
        FROM transactions
        WHERE isRecurring = 0 AND category = :category
        AND type = :type
        AND timestamp >= :sinceMillis
    """)
    suspend fun sumAmountByCategorySince(category: String, type: String, sinceMillis: Long): java.math.BigDecimal

    @Transaction
    suspend fun recalculateGoalProgress(goalId: Long, category: String, sinceMillis: Long = 0) {
        val spent = sumAmountByCategorySince(category, "DEBIT", sinceMillis)
        updateGoalProgress(goalId, spent)
    }

    @Query("""
        SELECT COALESCE(SUM(amount), '0.0')
        FROM transactions
        WHERE category = :category
        AND type = :type
    """)
    suspend fun sumAmountByCategory(category: String, type: String): java.math.BigDecimal
}
