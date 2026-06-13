package com.example.expncetracker.exptkr.ui.accounts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    val summary: StateFlow<FinancialSummary?> = getSummaryUseCase(DateFilter.MONTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _accounts = MutableStateFlow<List<AccountUiModel>>(emptyList())
    val accounts: StateFlow<List<AccountUiModel>> = _accounts.asStateFlow()

    fun addAccount(name: String, balance: Double, type: String) {
        val newAccount = AccountUiModel(
            name = name,
            balance = balance,
            type = type,
            icon = androidx.compose.material.icons.Icons.Default.AccountBalance,
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
        )
        _accounts.update { it + newAccount }
    }
}

data class AccountUiModel(
    val name: String,
    val balance: Double,
    val type: String,
    val icon: ImageVector,
    val color: Color
)
