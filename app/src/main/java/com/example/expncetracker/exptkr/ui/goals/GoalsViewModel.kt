package com.example.expncetracker.exptkr.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.GoalEntity
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository
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

    fun addGoal(name: String, targetAmount: Double, color: Int, deadline: Long? = null) {
        viewModelScope.launch {
            repository.insertGoal(
                GoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    color = color,
                    deadline = deadline
                )
            )
        }
    }

    fun updateGoalAmount(id: Long, additionalAmount: Double) {
        viewModelScope.launch {
            repository.getGoalById(id)?.let { goal ->
                val newAmount = goal.currentAmount + additionalAmount
                repository.updateGoal(
                    goal.copy(
                        currentAmount = newAmount,
                        isCompleted = newAmount >= goal.targetAmount
                    )
                )
            }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}
