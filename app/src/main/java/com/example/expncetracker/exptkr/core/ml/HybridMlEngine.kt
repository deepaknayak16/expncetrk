package com.example.expncetracker.exptkr.core.ml

import com.example.expncetracker.exptkr.core.common.MerchantNormalizer
import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.data.db.dao.MerchantStatsDao
import com.example.expncetracker.exptkr.data.db.entity.MerchantMappingEntity
import com.example.expncetracker.exptkr.di.ApplicationScope
import com.example.expncetracker.exptkr.domain.model.RecurringState
import com.example.expncetracker.exptkr.domain.model.Transaction
import com.example.expncetracker.exptkr.domain.model.TransactionType
import com.example.expncetracker.exptkr.domain.repository.RuleRepository
import com.example.expncetracker.exptkr.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridMlEngine @Inject constructor(
    private val tfIdfNaiveBayes: TfIdfNaiveBayes,
    private val recurringRegressor: RecurringProbabilityRegressor,
    private val amountAnomalyScorer: AmountAnomalyScorer,
    private val merchantMappingDao: MerchantMappingDao,
    private val merchantStatsDao: MerchantStatsDao,
    private val ruleRepository: RuleRepository,
    private val transactionRepository: TransactionRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {
    data class InferenceResult(
        val category: String,
        val confidenceScore: Float,
        val recurringState: RecurringState,
        val parsingStatus: String,
        val anomalyLevel: AmountAnomalyScorer.AnomalyLevel,
        val expectedAmount: Double?,
        val cleanMerchantName: String
    )

    suspend fun infer(
        merchantName: String,
        amount: BigDecimal,
        type: TransactionType,
        timestamp: Long,
        smsBody: String
    ): InferenceResult {

        // FIX BUG-GEN-05: Use standardized clean merchant name for ALL lookups
        val cleanMerchant = MerchantNormalizer.normalize(merchantName)

        // FIX BUG-ML-10: Eager training at start of infer to ensure model is ready
        if (!tfIdfNaiveBayes.isTrained()) {
            val history = transactionRepository.getRecentTransactions(200).first()
            if (history.isNotEmpty()) tfIdfNaiveBayes.train(history)
        }
        
        var finalCategory = "Others"
        var categoryConfidence = 0.0f

        // FIX BUG-5: Fetch rules once and reuse
        val allRules = ruleRepository.getActiveRulesList().sortedByDescending { it.priority }

        // -------------------------------------------------------------
        // PHASE 3: SMART DETECTION (Strict Priority Pipeline)
        // -------------------------------------------------------------

        // 1. User Mapping (Priority 100) - Checked against CLEAN merchant
        merchantMappingDao.getMappingForMerchant(cleanMerchant)?.let {
            finalCategory = it.categoryName
            categoryConfidence = 1.0f
        }

        // 2. High-Priority DB Rules (Priority 80+)
        if (categoryConfidence == 0.0f) {
            allRules.firstOrNull { 
                it.priority >= 80 && 
                (it.transactionType == null || it.transactionType == type.name) &&
                cleanMerchant.contains(it.keyword.uppercase(Locale.ROOT)) 
            }?.let {
                finalCategory = it.category
                categoryConfidence = 0.95f
            }
        }

        // 3. TF-IDF Naive Bayes (Only if highly confident)
        if (categoryConfidence == 0.0f) {
            val nbResult = tfIdfNaiveBayes.classify(cleanMerchant)
            
            // FIX BUG-ML-22: Adaptive confidence threshold based on training history
            val adaptiveThreshold = if (tfIdfNaiveBayes.isTrained()) 0.70f else 0.85f
            
            if (nbResult.confidence >= adaptiveThreshold && nbResult.source != TfIdfNaiveBayes.Source.FALLBACK) {
                finalCategory = nbResult.category
                categoryConfidence = nbResult.confidence
            }
        }

        // 4. Low-Priority DB Rules (Fallback catch-alls)
        if (categoryConfidence == 0.0f) {
            allRules.firstOrNull {
                it.priority < 80 && 
                (it.transactionType == null || it.transactionType == type.name) &&
                cleanMerchant.contains(it.keyword.uppercase(Locale.ROOT))
            }?.let {
                finalCategory = it.category
                categoryConfidence = 0.60f
            }
        }

        // -------------------------------------------------------------
        // PARALLEL TRACK: Welford's Stats Update (Numerical Stability)
        // -------------------------------------------------------------
        // Use cleanMerchant for stats too
        val stats = merchantStatsDao.getStats(cleanMerchant)
        val updatedStats = amountAnomalyScorer.updateStats(
            stats, cleanMerchant, finalCategory, amount, timestamp
        )
        merchantStatsDao.upsertStats(updatedStats)

        // -------------------------------------------------------------
        // PHASE 3 (Cont.): Recurring Regression
        // -------------------------------------------------------------
        val recurInput = RecurringProbabilityRegressor.FeatureInput(
            updatedStats, smsBody, finalCategory, dayOfMonth(timestamp)
        )
        val recurringState = recurringRegressor.classify(recurInput)

        // -------------------------------------------------------------
        // PHASE 4 & 8: Confidence & Anomaly Check
        // -------------------------------------------------------------
        val anomalyResult = amountAnomalyScorer.score(amount, stats)
        val anomalyPenalty = when (anomalyResult.level) {
            AmountAnomalyScorer.AnomalyLevel.NORMAL -> 0.0f
            AmountAnomalyScorer.AnomalyLevel.UNUSUAL -> 0.10f
            AmountAnomalyScorer.AnomalyLevel.ANOMALY -> 0.30f
            AmountAnomalyScorer.AnomalyLevel.ALERT -> 0.50f
        }
        val finalConfidence = (categoryConfidence - anomalyPenalty).coerceIn(0.1f, 1.0f)
        val parsingStatus = if (anomalyResult.level == AmountAnomalyScorer.AnomalyLevel.ANOMALY || 
                               anomalyResult.level == AmountAnomalyScorer.AnomalyLevel.ALERT || 
                               finalConfidence < 0.4f) "NEEDS_REVIEW" else "COMPLETE"

        return InferenceResult(
            category = finalCategory,
            confidenceScore = finalConfidence,
            recurringState = recurringState,
            parsingStatus = parsingStatus,
            anomalyLevel = anomalyResult.level,
            expectedAmount = anomalyResult.expectedAmount,
            cleanMerchantName = cleanMerchant
        )
    }

    /**
     * FIX BUG-ML-05: Single entry point for updates.
     */
    fun onCategoryCorrection(merchantName: String, correctCategory: String, wasManual: Boolean) {
        if (!wasManual) {
            android.util.Log.w("ML_ENGINE", "Attempted to auto-save ML guess to merchant_mappings. Blocked.")
            return
        }

        // Use cleaned merchant name for mapping key
        val cleanMerchant = MerchantNormalizer.normalize(merchantName)
        
        tfIdfNaiveBayes.markStale()
        
        appScope.launch {
            try {
                merchantMappingDao.upsertMapping(
                    MerchantMappingEntity(
                        merchantName = cleanMerchant, 
                        categoryName = correctCategory,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                com.example.expncetracker.exptkr.core.common.Logger.e("HybridMlEngine", "Failed to save mapping", e)
            }
        }
    }

    /**
     * PURGE: Clears poisoned database tables (mappings and stats) 
     * to allow the new improved ML logic to take over.
     * FIX BUG-GEN-06: Added reset() for in-memory model state.
     */
    suspend fun purgePoisonedData() {
        merchantMappingDao.nukeAllMappings()
        merchantStatsDao.nukeAllStats()
        tfIdfNaiveBayes.reset()
    }

    private fun dayOfMonth(epochMillis: Long): Int {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return cal.get(java.util.Calendar.DAY_OF_MONTH)
    }
}
