package com.example.expncetracker.exptkr.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expncetracker.exptkr.data.db.dao.AccountDao
import com.example.expncetracker.exptkr.data.db.dao.CategoryDao
import com.example.expncetracker.exptkr.domain.model.RecurrenceFrequency
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.GoalRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import com.example.expncetracker.exptkr.ui.accounts.AccountUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

// ─── UI state ─────────────────────────────────────────────────────────────────

data class AddTransactionUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

// ─── Merchant → category hint map ─────────────────────────────────────────────
// Extracted so it's testable and easy to extend without touching ViewModel logic

private val MERCHANT_CATEGORY_HINTS: List<Pair<Regex, String>> = listOf(
    Regex("swiggy|zomato|restaurant|cafe|food|kitchen|bakery", RegexOption.IGNORE_CASE) to "Food",
    Regex("grocery|supermarket|bigbasket|blinkit|zepto|dmart", RegexOption.IGNORE_CASE) to "Groceries",
    Regex("uber|ola|rapido|auto|cab|taxi|metro|bus|train", RegexOption.IGNORE_CASE) to "Cabs",
    Regex("amazon|flipkart|myntra|meesho|nykaa|ajio|shopping", RegexOption.IGNORE_CASE) to "Shopping",
    Regex("fuel|petrol|diesel|bharat petroleum|hp|ioc", RegexOption.IGNORE_CASE) to "Travel",
    Regex("rent|pgm|hostel|landlord|housing", RegexOption.IGNORE_CASE) to "Rent",
    Regex("netflix|hotstar|prime|spotify|youtube|entertainment", RegexOption.IGNORE_CASE) to "Entertainment",
    Regex("hospital|clinic|pharmacy|medicine|doctor|health", RegexOption.IGNORE_CASE) to "Healthcare",
    Regex("school|college|tuition|course|udemy|coursera|education", RegexOption.IGNORE_CASE) to "Education",
    Regex("electricity|water|gas|broadband|wifi|mobile|recharge|bill", RegexOption.IGNORE_CASE) to "Bills",
    Regex("salary|payroll|stipend|income|credit|employer", RegexOption.IGNORE_CASE) to "Salary",
    Regex("mutual fund|sip|stocks|zerodha|groww|invest|nps", RegexOption.IGNORE_CASE) to "Investments",
)

private fun suggestCategory(merchant: String): String? {
    if (merchant.isBlank()) return null
    return MERCHANT_CATEGORY_HINTS
        .firstOrNull { (pattern, _) -> pattern.containsMatchIn(merchant) }
        ?.second
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val goalRepository: GoalRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val merchantMappingDao: com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
) : ViewModel() {

    // ── Exposed state ──────────────────────────────────────────────────────────

    val accounts: StateFlow<List<AccountUiModel>> = accountDao.getAllAccounts()
        .map { list ->
            list.map { AccountUiModel(it.id, it.name, it.balance, it.type, it.color) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _transactionToEdit = MutableStateFlow<Transaction?>(null)
    val transactionToEdit: StateFlow<Transaction?> = _transactionToEdit.asStateFlow()

    private val _suggestedCategory = MutableStateFlow<String?>(null)
    val suggestedCategory: StateFlow<String?> = _suggestedCategory.asStateFlow()

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    // FIX: SharedFlow for one-shot events — no channel leak, works correctly
    // with Compose's LaunchedEffect(Unit) { collect {} } pattern
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

    // FIX: pure function — no coroutine needed, no IO, just string matching
    // FIX: clears suggestion when merchant is blank
    // FIX: doesn't overwrite a user-selected category (caller passes userSelectedCategory)
    fun onMerchantNameChanged(merchant: String, currentCategory: String, userSelectedCategory: Boolean) {
        if (merchant.isBlank()) {
            _suggestedCategory.value = null
            return
        }
        if (userSelectedCategory) return  // don't stomp on explicit user choice
        val suggestion = suggestCategory(merchant)
        if (suggestion != null && (currentCategory.isEmpty() || currentCategory == "Others")) {
            _suggestedCategory.value = suggestion
        }
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    // FIX: parameter name is `merchant` throughout (was `description` in UI, `merchant` here — now consistent)
    // FIX: validation lives here, not in the composable
    // FIX: goal recalculation extracted to avoid duplication
    // FIX: clearEdit() called on both insert and update paths
    // FIX: isSaving state prevents double-tap saves
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
        // Guard: prevent double-save
        if (_uiState.value.isSaving) return

        // Evaluate and Validate
        val amountResult = runCatching { evaluate(amountText) }
        val amount = amountResult.getOrDefault(-1.0)

        val validationError = when {
            amountResult.isFailure -> "Invalid expression"
            amount <= 0.0 -> "Amount must be greater than zero"
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
                // Resolve account ID: prefer explicit accountId, fall back to name lookup
                val resolvedAccountId = when {
                    accountId != 0L -> accountId
                    else -> accountDao.getAccountIdByName(bankName) ?: 0L
                }

                val transaction = Transaction(
                    id = id,
                    smsId = _transactionToEdit.value?.smsId,
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
                    tags = tags
                )

                if (id != 0L) {
                    repository.updateTransactionWithBalance(_transactionToEdit.value, transaction)
                    "Transaction updated"
                } else {
                    repository.insertTransactionWithBalance(transaction)
                    "Transaction added"
                }
            }.onSuccess { message ->
                // Save merchant mapping (Phase 3 Learning)
                viewModelScope.launch {
                    if (merchant.isNotBlank() && category.isNotBlank()) {
                        merchantMappingDao.insertMapping(
                            com.example.expncetracker.exptkr.data.db.entity.MerchantMappingEntity(
                                merchantName = merchant.trim(),
                                categoryName = category
                            )
                        )
                    }
                }
                
                // FIX: recalculation on both paths, once
                    if (category == "Savings" || category == "Investment") {
                        runCatching { goalRepository.recalculateGoalsByCategory(category) }
                    }
                    // FIX: Don't call clearEdit() immediately as it resets isSaved
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

    // ── Helpers ────────────────────────────────────────────────────────────────

    // FIX: nextDueDate was always set to `timestamp` — now calculates the actual next occurrence
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

    // ── Validation helpers (for UI to gate the Save button) ───────────────────

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