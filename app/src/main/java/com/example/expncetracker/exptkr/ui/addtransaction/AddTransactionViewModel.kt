package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.core.ml.HybridMlEngine
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.*
import com.example.expncetracker.exptkr.ui.accounts.AccountUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

// ─── UI state ─────────────────────────────────────────────────────────────────

data class AddTransactionUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val mlEngine: HybridMlEngine
) : ViewModel() {

    // ── Exposed state ──────────────────────────────────────────────────────────

    val accounts: StateFlow<List<AccountUiModel>> = accountRepository.getAllAccounts()
        .map { list ->
            list.map { AccountUiModel(it.id, it.name, it.balance, it.type, it.color) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    val transactionToEdit: StateFlow<Transaction?> = _transactionToEdit.asStateFlow()

    private val _suggestedCategory = MutableStateFlow<String?>(null)
    val suggestedCategory: StateFlow<String?> = _suggestedCategory.asStateFlow()

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _statusEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val statusEvent: SharedFlow<String> = _statusEvent.asSharedFlow()

    // ── Load ───────────────────────────────────────────────────────────────────

    fun loadTransaction(transactionId: Long) {
        if (transactionId <= 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { repository.getTransactionById(transactionId) }
                .onSuccess { tx ->
                    _transactionToEdit.value = tx
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearEdit() {
        _transactionToEdit.value = null
        _suggestedCategory.value = null
        _uiState.value = AddTransactionUiState()
    }

    // ── Merchant hint ──────────────────────────────────────────────────────────

    /**
     * FIX BUG-ML-06: Use ML Engine for suggestions instead of rules-only.
     */
    fun onMerchantNameChanged(merchant: String, currentCategory: String, userSelectedCategory: Boolean, type: TransactionType) {
        if (merchant.isBlank()) {
            _suggestedCategory.value = null
            return
        }
        if (userSelectedCategory) return 

        viewModelScope.launch {
            val result = mlEngine.infer(
                merchantName = merchant,
                amount = BigDecimal.ZERO,
                type = type,
                timestamp = System.currentTimeMillis(),
                smsBody = ""
            )

            val cat = currentCategory.trim().lowercase()
            val isOthers = cat == "others" || cat == "other"

            // Only suggest if ML is confident (> 0.5 for suggestion)
            if (result.confidenceScore >= 0.5f && (currentCategory.isBlank() || isOthers)) {
                _suggestedCategory.value = result.category
            }
        }
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    fun addTransaction(
        id: Long = 0L,
        amountText: String,
        type: TransactionType,
        category: String,
        merchant: String,
        note: String? = null,
        bankName: String,
        accountId: Long = 0L,
        counterparty: String? = null,
        isRecurring: Boolean = false,
        frequency: RecurrenceFrequency? = null,
        tags: List<String> = emptyList(),
        timestamp: LocalDateTime = LocalDateTime.now()
    ) {
        if (_uiState.value.isSaving) return

        val amountResult = runCatching { evaluate(amountText).toBigDecimal() }
        val amount = amountResult.getOrDefault(BigDecimal.valueOf(-1))

        val validationError = when {
            amountResult.isFailure -> "Invalid expression"
            amount.signum() <= 0 -> "Amount must be greater than zero"
            merchant.isBlank() -> "Merchant / payee is required"
            category.isBlank() -> "Please select a category"
            bankName.isBlank() -> "Please select an account"
            isRecurring && frequency == null -> "Select a recurrence frequency"
            else -> null
        }

        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            viewModelScope.launch { _statusEvent.emit(validationError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val resolvedAccountId = when {
                    accountId != 0L -> accountId
                    else -> accountRepository.getAccountByName(bankName)?.id ?: 0L
                }

                val oldTx = _transactionToEdit.value
                
                // If it's an update, we assume category was manually reviewed/corrected
                val finalIsCategoryManuallyCorrected = (oldTx?.isCategoryManuallyCorrected == true) || (id != 0L)

                val transaction = Transaction(
                    id = id,
                    smsId = oldTx?.smsId,
                    accountId = resolvedAccountId,
                    amount = amount,
                    type = type,
                    categoryName = category,
                    merchant = merchant,
                    bankName = bankName,
                    note = note.takeIf { !it.isNullOrBlank() },
                    timestamp = timestamp,
                    isRecurring = isRecurring,
                    frequency = frequency,
                    nextDueDate = if (isRecurring) calculateNextDueDate(timestamp, frequency) else null,
                    counterparty = counterparty.takeIf { !it.isNullOrBlank() },
                    tags = tags,
                    isCategoryManuallyCorrected = finalIsCategoryManuallyCorrected,
                    cleanMerchantName = oldTx?.cleanMerchantName // will be re-normalized in repository if needed
                )

                if (id != 0L) {
                    repository.updateTransactionWithBalance(_transactionToEdit.value, transaction)
                    "Transaction updated"
                } else {
                    repository.insertTransactionWithBalance(transaction)
                    "Transaction added"
                }
            }.onSuccess { message ->
                // FIX BUG-ML-05: Use mlEngine.onCategoryCorrection with strict wasManual flag
                // We only learn if the user explicitly picked this category or edited an existing one
                mlEngine.onCategoryCorrection(
                    merchantName = merchant.trim(),
                    correctCategory = category,
                    wasManual = true // This method is only called from UI saves
                )

                // FIX BUG-ML-07: Standardized category names for triggers
                if (category == "Investments" || category == "Savings") {
                    runCatching { goalRepository.recalculateGoalsByCategory(category) }
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
                _statusEvent.emit(message)
            }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Something went wrong")
                    }
                    _statusEvent.emit("Failed to save transaction")
                }
        }
    }

    private fun calculateNextDueDate(
        from: LocalDateTime,
        frequency: RecurrenceFrequency?
    ): LocalDateTime? = when (frequency) {
        RecurrenceFrequency.DAILY -> from.plusDays(1)
        RecurrenceFrequency.WEEKLY -> from.plusWeeks(1)
        RecurrenceFrequency.MONTHLY -> from.plusMonths(1)
        RecurrenceFrequency.YEARLY -> from.plusYears(1)
        null -> null
    }

    fun isFormValid(
        amount: String,
        merchant: String,
        category: String,
        bankName: String
    ): Boolean {
        val parsedAmount = runCatching { evaluate(amount) }.getOrDefault(0.0)
        return parsedAmount > 0.0 &&
                merchant.isNotBlank() &&
                category.isNotBlank() &&
                bankName.isNotBlank()
    }

    private fun evaluate(expression: String): Double {
        val expr = expression.trim()
            .replace("÷", "/")
            .replace("×", "*")
            .replace("−", "-")
        if (expr.isEmpty()) return 0.0
        return object : Any() {
            var pos = -1; var ch = 0
            fun nextChar() { ch = if (++pos < expr.length) expr[pos].code else -1 }
            fun eat(c: Int): Boolean {
                while (ch == ' '.code) nextChar()
                return if (ch == c) { nextChar(); true } else false
            }
            fun parse(): Double { nextChar(); return parseExpr().also { if (pos < expr.length) throw RuntimeException("Unexpected: ${ch.toChar()}") } }
            fun parseExpr(): Double { var x = parseTerm(); while (true) x = when { eat('+'.code) -> x + parseTerm(); eat('-'.code) -> x - parseTerm(); else -> return x }; @Suppress("UNREACHABLE_CODE") return x }
            fun parseTerm(): Double { var x = parseFactor(); while (true) x = when { eat('*'.code) -> x * parseFactor(); eat('/'.code) -> parseFactor().let { d -> if (d == 0.0) throw ArithmeticException("Div by zero"); x / d }; eat('%'.code) -> (x * parseFactor()) / 100.0; else -> return x }; @Suppress("UNREACHABLE_CODE") return x }
            fun parseFactor(): Double { if (eat('+'.code)) return parseFactor(); if (eat('-'.code)) return -parseFactor(); val start = pos; return if (eat('('.code)) parseExpr().also { eat(')'.code) } else if (ch in '0'.code..'9'.code || ch == '.'.code) { while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar(); expr.substring(start, pos).toDouble() } else throw RuntimeException("Unexpected: ${ch.toChar()}") }
        }.parse()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
