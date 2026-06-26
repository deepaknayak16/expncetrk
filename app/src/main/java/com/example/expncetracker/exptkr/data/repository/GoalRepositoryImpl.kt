package com.example.expncetracker.exptkr.data.repository

import androidx.room.withTransaction
import com.example.expncetracker.exptkr.data.db.AppDatabase
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.GoalDao
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

    // FIX #10: Balance check moved inside transaction; floor applied
    override suspend fun contributeToGoal(goalId: Long, amount: BigDecimal) {
        val goal = goalDao.getGoalById(goalId) ?: return
        val accountId = goal.linkedAccountId

        if (accountId == null) {
            val newAmount = goal.currentAmount.add(amount).max(BigDecimal.ZERO)
            goalDao.updateGoal(
                goal.copy(
                    currentAmount = newAmount,
                    isCompleted = newAmount >= goal.targetAmount
                )
            )
            return
        }

        // FIX #10: Everything inside withTransaction for atomicity
        db.withTransaction {
            val account = accountDao.getAccountById(accountId)
                ?: throw IllegalStateException("Linked account not found")

            if (account.balance < amount && amount > BigDecimal.ZERO) {
                throw IllegalStateException("Insufficient balance in ${account.name}")
            }

            accountDao.adjustBalanceById(accountId, amount.negate())
            val newAmount = goal.currentAmount.add(amount).max(BigDecimal.ZERO) // FIX #10: floor
            goalDao.updateGoal(
                goal.copy(
                    currentAmount = newAmount,
                    isCompleted = newAmount >= goal.targetAmount
                )
            )
        }
    }

    // FIX #9: Skip account-tracking goals — category recalc is only for virtual tracking
    override suspend fun recalculateGoalProgress(goalId: Long) {
        val goal = goalDao.getGoalById(goalId) ?: return
        if (goal.linkedAccountId != null) return // FIX #9: account-tracking goals use contributeToGoal, not category sums
        val category = goal.linkedCategory ?: return
        goalDao.recalculateGoalProgress(goalId, category, goal.createdAt)
    }

    override suspend fun recalculateGoalsByCategory(category: String) {
        val goals = goalDao.getGoalsByCategory(category)
        goals.forEach { goal ->
            if (goal.linkedAccountId == null) { // FIX #9: only recalc virtual-tracking goals
                goal.linkedCategory?.let { cat ->
                    goalDao.recalculateGoalProgress(goal.id, cat, goal.createdAt)
                }
            }
        }
    }
}
