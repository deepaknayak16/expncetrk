package com.example.expncetracker.exptkr.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.entity.CategoryEntity
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.model.DateFilter
import com.example.expncetracker.exptkr.domain.repository.CategoryRepository
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    init {
        // Seeding handled by DatabaseModule callback
    }

    private val _selectedFilter = MutableStateFlow(DateFilter.MONTH)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow(java.time.LocalDateTime.now())
    val selectedDate = _selectedDate.asStateFlow()

    val summary: StateFlow<FinancialSummary?> = combine(_selectedFilter, _selectedDate) { filter, date ->
        filter to date
    }.flatMapLatest { (filter, date) ->
        val zone = java.time.ZoneId.systemDefault()
        val startMillis: Long
        val endMillis: Long
        
        when (filter) {
            DateFilter.DAY -> {
                startMillis = date.withHour(0).withMinute(0).withSecond(0).atZone(zone).toInstant().toEpochMilli()
                endMillis = date.withHour(23).withMinute(59).withSecond(59).atZone(zone).toInstant().toEpochMilli()
            }
            DateFilter.WEEK -> {
                val start = date.minusDays(6).withHour(0).withMinute(0)
                startMillis = start.atZone(zone).toInstant().toEpochMilli()
                endMillis = date.withHour(23).withMinute(59).withSecond(59).atZone(zone).toInstant().toEpochMilli()
            }
            DateFilter.MONTH -> {
                val start = date.withDayOfMonth(1).withHour(0).withMinute(0)
                val end = date.withDayOfMonth(date.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                startMillis = start.atZone(zone).toInstant().toEpochMilli()
                endMillis = end.atZone(zone).toInstant().toEpochMilli()
            }
            DateFilter.YEAR -> {
                val start = date.withDayOfYear(1).withHour(0).withMinute(0)
                val end = date.withDayOfYear(date.toLocalDate().lengthOfYear()).withHour(23).withMinute(59)
                startMillis = start.atZone(zone).toInstant().toEpochMilli()
                endMillis = end.atZone(zone).toInstant().toEpochMilli()
            }
            else -> {
                startMillis = date.withHour(0).atZone(zone).toInstant().toEpochMilli()
                endMillis = date.atZone(zone).toInstant().toEpochMilli()
            }
        }
        getSummaryUseCase(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    fun updateDate(newDate: java.time.LocalDateTime) {
        _selectedDate.value = newDate
    }

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
