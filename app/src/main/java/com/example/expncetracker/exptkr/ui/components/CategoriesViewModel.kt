package com.example.expncetracker.exptkr.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.repository.CategoryRepository
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    init {
        // Seeding handled by DatabaseModule callback
    }

    val summary: StateFlow<FinancialSummary?> = getSummaryUseCase(DateFilter.MONTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            categoryRepository.insertCategory(
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
