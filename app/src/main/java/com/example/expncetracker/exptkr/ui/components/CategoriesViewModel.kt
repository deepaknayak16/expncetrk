package com.example.expncetracker.exptkr.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    val summary: StateFlow<FinancialSummary?> = getSummaryUseCase(DateFilter.MONTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    // We still use the enum for legacy data, but we can combine it with DB categories
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate if empty
        viewModelScope.launch {
            if (categoryDao.getAllCategories().first().isEmpty()) {
                Category.entries.forEach { cat ->
                    val type = if (cat in listOf(Category.SALARY, Category.INVESTMENTS)) "INCOME" else "EXPENSE"
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = cat.displayName,
                            type = type,
                            iconName = cat.name,
                            color = 0xFF3B82F6.toInt() // Default blue
                        )
                    )
                }
            }
        }
    }

    fun triggerAddCategory() {
        _showAddDialog.value = true
    }

    fun onDialogDismissed() {
        _showAddDialog.value = false
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            categoryDao.insertCategory(
                CategoryEntity(
                    name = name,
                    type = type,
                    iconName = "OTHERS",
                    color = 0xFF3B82F6.toInt()
                )
            )
        }
    }
}
