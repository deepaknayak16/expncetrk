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


    @Query("""
        UPDATE goals
        SET currentAmount = (
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE categoryName = :category AND type = 'DEBIT'
        ),
        isCompleted = (
            SELECT COALESCE(SUM(amount), 0)
            FROM transactions
            WHERE categoryName = :category AND type = 'DEBIT'
        ) >= targetAmount
        WHERE id = :goalId
    """)
    suspend fun recalculateGoalProgress(goalId: Long, category: String)

    @Query("""
    SELECT COALESCE(SUM(amount), 0.0)
    FROM transactions
    WHERE categoryName = :category
    AND type = :type
    """)
    suspend fun sumAmountByCategory(category: String, type: String): Double
}
