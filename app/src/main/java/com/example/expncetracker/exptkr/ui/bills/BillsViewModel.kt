package com.example.expncetracker.exptkr.ui.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.RecurringTemplateDao
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import com.example.expncetracker.exptkr.domain.model.RecurringState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillsViewModel @Inject constructor(
    private val recurringTemplateDao: RecurringTemplateDao
) : ViewModel() {

    val bills: StateFlow<List<RecurringTemplateEntity>> = recurringTemplateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateBillState(billId: Long, newState: RecurringState) {
        viewModelScope.launch {
            val bill = bills.value.find { it.id == billId } ?: return@launch
            recurringTemplateDao.update(bill.copy(state = newState.name))
        }
    }

    fun deleteBill(billId: Long) {
        updateBillState(billId, RecurringState.CANCELLED)
    }
}
