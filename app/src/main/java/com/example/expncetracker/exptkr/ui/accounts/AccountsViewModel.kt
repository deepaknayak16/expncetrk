package com.example.expncetracker.exptkr.ui.accounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.entity.AccountEntity
import com.example.expncetracker.exptkr.domain.model.FinancialSummary
import com.example.expncetracker.exptkr.domain.usecase.GetSummaryUseCase
import com.example.expncetracker.exptkr.ui.dashboard.DateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    getSummaryUseCase: GetSummaryUseCase
) : ViewModel() {

    val summary: StateFlow<FinancialSummary?> = getSummaryUseCase(DateFilter.MONTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<AccountUiModel>> = accountDao.getAllAccounts()
        .map { entities ->
            entities.map { entity ->
                AccountUiModel(
                    id = entity.id,
                    name = entity.name,
                    balance = entity.balance,
                    type = entity.type,
                    icon = Icons.Default.AccountBalance,
                    color = Color(entity.color)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAccount(name: String, balance: Double, type: String) {
        viewModelScope.launch {
            accountDao.insertAccount(
                AccountEntity(
                    name = name,
                    balance = balance,
                    type = type,
                    color = Color(0xFF4CAF50).toArgb()
                )
            )
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            accountDao.deleteAccountById(id)
        }
    }
}

data class AccountUiModel(
    val id: Long = 0,
    val name: String,
    val balance: Double,
    val type: String,
    val icon: ImageVector,
    val color: Color
)
