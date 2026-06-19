package com.example.expncetracker.exptkr.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository
    // REMOVED: private val transactionRepository — no longer used directly
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    private val _statusEvent = Channel<String>(Channel.BUFFERED)
    val statusEvent = _statusEvent.receiveAsFlow()

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
        linkedCategory: String? = null,
        linkedAccountId: Long? = null
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
            try {
                repository.contributeToGoal(id, amount)
                _statusEvent.send("₹$amount moved to goal")
            } catch (e: IllegalStateException) {
                _statusEvent.send(e.message ?: "Contribution failed")
            } catch (e: Exception) {
                _statusEvent.send("Contribution failed: ${e.message}")
            }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun recalculateGoalProgress(goalId: Long) {
        viewModelScope.launch {
            try {
                repository.recalculateGoalProgress(goalId)
            } catch (e: Exception) {
                _statusEvent.send("Goal recalculation failed: ${e.localizedMessage}")
            }
        }
    }
}
