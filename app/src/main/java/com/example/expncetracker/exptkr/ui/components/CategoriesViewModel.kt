package com.example.expncetracker.exptkr.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.model.Category
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-populate if empty with correct colors
        viewModelScope.launch {
            if (categoryDao.getAllCategories().first().isEmpty()) {
                Category.entries.forEach { cat ->
                    val type = if (cat in listOf(Category.SALARY, Category.INVESTMENTS)) "INCOME" else "EXPENSE"
                    val color = when (cat) {
                        Category.FOOD -> 0xFFF97316
                        Category.CABS -> 0xFF6366F1
                        Category.RENT -> 0xFFEAB308
                        Category.BILLS -> 0xFFEC4899
                        Category.SHOPPING -> 0xFFA855F7
                        Category.SALARY -> 0xFF10B981
                        Category.INVESTMENTS -> 0xFF06B6D4
                        Category.TRAVEL -> 0xFF3B82F6
                        Category.ENTERTAINMENT -> 0xFF8B5CF6
                        Category.GROCERIES -> 0xFF4ADE80
                        Category.HEALTHCARE -> 0xFFEF4444
                        Category.EDUCATION -> 0xFF14B8A6
                        Category.OTHERS -> 0xFF64748B
                    }.toInt()
                    
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = cat.displayName,
                            type = type,
                            iconName = cat.name,
                            color = color
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

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Categories refresh is just a local state refresh trigger if needed
                kotlinx.coroutines.delay(300)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addCategory(name: String, type: String, iconName: String, color: Int) {
        viewModelScope.launch {
            categoryDao.insertCategory(
                CategoryEntity(
                    name = name,
                    type = type,
                    iconName = iconName,
                    color = color
                )
            )
        }
    }
}
