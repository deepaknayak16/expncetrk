package com.example.expncetracker.exptkr.core.ml

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.expncetracker.exptkr.core.common.RECURRING_ML_WEIGHTS_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.data.db.entity.MerchantStatsEntity
import com.example.expncetracker.exptkr.domain.model.RecurringState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * Logistic Regression — recurring probability score.
 * 
 * FIX 1: Added @Synchronized to prevent race conditions during online learning (SGD).
 * FIX BUG-ML-04: Added DataStore persistence for learned weights.
 */
@Singleton
class RecurringProbabilityRegressor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Serializable
    data class WeightsSnapshot(val weights: List<Float>, val bias: Float)

    // Thresholds for the three-state machine
    val CONFIRMED_THRESHOLD = 0.80f
    val LIKELY_THRESHOLD    = 0.45f

    // Pre-tuned weights [f0..f7] + bias
    private val weights = floatArrayOf(
        0.55f,  // f0: occurrence count
        0.70f,  // f1: interval regularity
        0.40f,  // f2: amount stability
        1.00f,  // f3: category prior
        1.30f,  // f4: keyword score
        1.80f,  // f5: hard auto-debit flag
        1.50f,  // f6: due-date mention
        0.35f   // f7: day-of-month regularity
    )
    private var bias = -2.2f

    init {
        // Load saved weights on init
        CoroutineScope(Dispatchers.IO).launch {
            val saved = context.dataStore.data.first()[RECURRING_ML_WEIGHTS_KEY]
            saved?.let { json ->
                runCatching {
                    val snapshot = Json.decodeFromString<WeightsSnapshot>(json)
                    snapshot.weights.forEachIndexed { i, v -> if (i < weights.size) weights[i] = v }
                    bias = snapshot.bias
                }
            }
        }
    }

    @Synchronized
    private fun persistWeights() {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                val snapshot = WeightsSnapshot(weights.toList(), bias)
                prefs[RECURRING_ML_WEIGHTS_KEY] = Json.encodeToString(snapshot)
            }
        }
    }

    @Synchronized
    fun predict(input: FeatureInput): Float {
        val f = buildFeatureVector(input)
        val logit = f.zip(weights.toTypedArray()).sumOf { (feat, w) ->
            (feat * w).toDouble()
        }.toFloat() + bias
        return sigmoid(logit)
    }

    @Synchronized
    fun classify(input: FeatureInput): RecurringState {
        val p = predict(input)
        return when {
            p >= CONFIRMED_THRESHOLD -> RecurringState.ACTIVE
            p >= LIKELY_THRESHOLD -> RecurringState.PENDING_CONFIRM
            else -> RecurringState.NONE
        }
    }

    /**
     * Update weights via SGD.
     * 
     * FIX: Added L2 regularization to prevent weight divergence.
     */
    @Synchronized
    fun onUserFeedback(input: FeatureInput, wasActuallyRecurring: Boolean) {
        val f = buildFeatureVector(input)
        val predicted = predict(input)
        val actual = if (wasActuallyRecurring) 1f else 0f
        val error = predicted - actual

        for (i in weights.indices) {
            // Added 0.001f L2 penalty as suggested in the review
            weights[i] -= learningRate * (error * f[i] + 0.001f * weights[i])
        }
        bias -= learningRate * error

        for (i in weights.indices) {
            weights[i] = weights[i].coerceIn(-5f, 5f)
        }
        
        persistWeights()
    }

    private val learningRate = 0.02f

    data class FeatureInput(
        val stats: MerchantStatsEntity?,
        val smsBody: String,
        val category: String,
        val dayOfMonth: Int
    )

    private fun buildFeatureVector(input: FeatureInput): FloatArray {
        val stats = input.stats
        val f0 = if (stats != null) (stats.amountCount / 10f).coerceIn(0f, 1f) else 0f
        val f1 = if (stats != null && stats.intervalCount >= 2)
            (1f - stats.intervalCV.toFloat()).coerceIn(0f, 1f)
        else 0f
        val f2 = if (stats != null && stats.amountCount >= 2)
            (1f - stats.amountCV.toFloat()).coerceIn(0f, 1f)
        else 0f
        val f3 = categoryPrior(input.category)
        val f4 = keywordScore(input.smsBody)
        val f5 = if (autoDebitKeywords.any { input.smsBody.contains(it, ignoreCase = true) }) 1f else 0f
        val f6 = if (dueDateKeywords.any { input.smsBody.contains(it, ignoreCase = true) }) 1f else 0f
        val f7 = if (stats != null && stats.amountCount >= 2) dayOfMonthScore(input.dayOfMonth) else 0f

        return floatArrayOf(f0, f1, f2, f3, f4, f5, f6, f7)
    }

    private fun categoryPrior(category: String): Float = when (category) {
        "Subscriptions"  -> 0.95f
        "Loan & EMI"     -> 0.95f
        "Insurance"      -> 0.90f
        "Bills"          -> 0.85f
        "Rent"           -> 0.85f
        "Investments"    -> 0.80f
        "Education"      -> 0.50f
        "Healthcare"     -> 0.20f
        "Food"           -> 0.05f
        "Shopping"       -> 0.05f
        "Cabs"           -> 0.03f
        else             -> 0.10f
    }

    private fun keywordScore(smsBody: String): Float {
        val upper = smsBody.uppercase()
        var score = 0f
        for ((keyword, weight) in keywordWeights) {
            if (upper.contains(keyword)) score += weight
        }
        return score.coerceIn(0f, 1f)
    }

    private fun dayOfMonthScore(day: Int): Float = when (day) {
        1, 2, 3         -> 0.8f
        4, 5, 6         -> 0.7f
        7, 8, 9, 10     -> 0.6f
        15, 16          -> 0.7f
        25, 26, 27, 28  -> 0.6f
        else            -> 0.2f
    }

    private val keywordWeights = mapOf(
        "EMI"                   to 0.8f,
        "SIP"                   to 0.8f,
        "INSTALMENT"            to 0.8f,
        "INSTALLMENT"           to 0.8f,
        "SUBSCRIPTION"          to 0.7f,
        "MONTHLY"               to 0.5f,
        "ANNUALLY"              to 0.5f,
        "AUTO PAY"              to 0.7f,
        "MANDATE"               to 0.8f,
        "NACH"                  to 0.9f,
        "PREMIUM"               to 0.5f,
        "RENEWAL"               to 0.6f,
        "RECHARGE"              to 0.4f,
        "BILL PAYMENT"          to 0.5f,
        "UTILITY"               to 0.4f,
    )

    private val autoDebitKeywords = listOf(
        "AUTO-DEBIT", "AUTO DEBIT", "AUTODEBIT",
        "STANDING INSTRUCTION", "STANDING INSTR",
        "ECS DEBIT", "ECS DR",
        "NACH DEBIT", "NACH DR"
    )

    private val dueDateKeywords = listOf(
        "DUE ON", "DUE DATE", "DUE BY",
        "PAY BY", "PAYMENT DUE", "LAST DATE"
    )

    private fun sigmoid(x: Float) = (1.0 / (1.0 + exp(-x.toDouble()))).toFloat()
}
