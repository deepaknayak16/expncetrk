package com.example.expncetracker.exptkr.data.repository

import androidx.room.withTransaction
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.GoalDao
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val accountDao: AccountDao,
    private val db: AppDatabase
) : GoalRepository {

    override fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    override suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    override suspend fun insertGoal(goal: GoalEntity) = goalDao.insertGoal(goal)

    override suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    override suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)

    // FIX #14: Atomic contribute — deduct from account + add to goal in one transaction
    override suspend fun contributeToGoal(goalId: Long, amount: Double) {
        val goal = goalDao.getGoalById(goalId) ?: return
        val accountId = goal.linkedAccountId

        if (accountId == null) {
            // No linked account — just update the virtual counter
            val newAmount = (goal.currentAmount + amount).coerceAtLeast(0.0)
            goalDao.updateGoal(
                goal.copy(
                    currentAmount = newAmount,
                    isCompleted = newAmount >= goal.targetAmount
                )
            )
            return
        }

        val account = accountDao.getAccountById(accountId)
            ?: throw IllegalStateException("Linked account not found")

        if (account.balance < amount) {
            throw IllegalStateException("Insufficient balance in ${account.name}")
        }

        db.withTransaction {
            accountDao.adjustBalanceById(accountId, -amount)
            goalDao.updateGoal(
                goal.copy(currentAmount = goal.currentAmount + amount)
            )
        }
    }

    // FIX #15: Recalculate one goal's progress from its linked category
    override suspend fun recalculateGoalProgress(goalId: Long) {
        val goal = goalDao.getGoalById(goalId) ?: return
        val category = goal.linkedCategory ?: return
        goalDao.recalculateGoalProgress(goalId, category)
    }

    // FIX #17: Recalculate all goals linked to a given category
    override suspend fun recalculateGoalsByCategory(category: String) {
        val goals = goalDao.getGoalsByCategory(category)
        goals.forEach { goal ->
            goal.linkedCategory?.let { cat ->
                goalDao.recalculateGoalProgress(goal.id, cat)
            }
        }
    }
}
