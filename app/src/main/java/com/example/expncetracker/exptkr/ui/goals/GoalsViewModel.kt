package com.example.expncetracker.exptkr.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository,
    private val transactionRepository: TransactionRepository  // <-- ADD
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    fun triggerAddGoal() {
        _showAddDialog.value = true
    }

    fun onDialogDismissed() {
        _showAddDialog.value = false
    }

    fun addGoal(
            name: String,
            targetAmount: Double,
            color: Int,
            deadline: Long? = null,
            linkedCategory: String? = null,   // NEW
            linkedAccountId: Long? = null      // NEW
        ) {
        viewModelScope.launch {
            repository.insertGoal(
                GoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    color = color,
                    deadline = deadline,
                    linkedCategory = linkedCategory,
                    linkedAccountId = linkedAccountId
                )
            )
        }
    }

    fun updateGoalAmount(id: Long, additionalAmount: Double) {
        viewModelScope.launch {
            repository.getGoalById(id)?.let { goal ->
                val newAmount = (goal.currentAmount + additionalAmount).coerceAtLeast(0.0)
                repository.updateGoal(
                    goal.copy(
                        currentAmount = newAmount,
                        isCompleted = newAmount >= goal.targetAmount
                    )
                )
            }
        }
    }

    fun contributeToGoal(id: Long, amount: Double) {
        viewModelScope.launch {
            val goal = repository.getGoalById(id) ?: return@launch
            val accountId = goal.linkedAccountId

            if (accountId == null) {
                // No account linked — just update the virtual counter
                val newAmount = (goal.currentAmount + amount).coerceAtLeast(0.0)
                repository.updateGoal(goal.copy(currentAmount = newAmount, isCompleted = newAmount >= goal.targetAmount))
                return@launch
            }

            // Deduct from the linked account
            val account = accountDao.getAccountById(accountId) ?: return@launch
            if (account.balance < amount) {
                _statusEvent.send("Insufficient balance in ${account.name}")
                return@launch
            }

            // Atomic: deduct from account + add to goal
            db.withTransaction {
                accountDao.adjustBalanceById(accountId, -amount) // Deduct
                repository.updateGoal(goal.copy(currentAmount = goal.currentAmount + amount))
            }

            _statusEvent.send("₹$amount moved to ${goal.name}")
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // WHY: Scan all transactions in the linked category and sum them
    // so the goal progress updates automatically.

    fun recalculateGoalProgress(goalId: Long) {
        viewModelScope.launch {
            try {
                val goal = repository.getGoalById(goalId) ?: return@launch
                val category = goal.linkedCategory ?: return@launch

                // Determine which transaction type contributes to this goal
                val targetType = when (goal.goalType) {
                    GoalType.SAVINGS -> TransactionType.CREDIT.name
                    else -> TransactionType.DEBIT.name
                }

                val total = transactionRepository.sumByCategory(category, targetType)

                repository.updateGoal(
                    goal.copy(
                        currentAmount = total,
                        isCompleted = total >= goal.targetAmount
                    )
                )
            } catch (e: Exception) {
                _statusEvent.send("Goal recalculation failed: ${e.localizedMessage}")
            }
        }
    }
}
