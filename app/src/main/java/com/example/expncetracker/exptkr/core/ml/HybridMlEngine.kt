package com.example.expncetracker.exptkr.core.ml

import com.example.expncetracker.exptkr.data.db.dao.MerchantMappingDao
import com.example.expncetracker.exptkr.data.db.dao.MerchantStatsDao
import com.example.expncetracker.exptkr.data.db.entity.MerchantMappingEntity
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
    private val transactionRepository: TransactionRepository
) {
    data class InferenceResult(
        val category: String,
        val confidenceScore: Float,
        val recurringState: RecurringState,
        val parsingStatus: String,
        val anomalyLevel: AmountAnomalyScorer.AnomalyLevel,
        val expectedAmount: Double?
    )

    suspend fun infer(
        merchantName: String,
        amount: BigDecimal,
        type: TransactionType,
        timestamp: Long,
        smsBody: String
    ): InferenceResult {

        val upper = merchantName.uppercase(Locale.ROOT)
        var finalCategory = "Others"
        var categoryConfidence = 0.0f
        var categorySource = TfIdfNaiveBayes.Source.FALLBACK

        // -------------------------------------------------------------
        // PHASE 3: SMART DETECTION (Strict Priority Pipeline)
        // -------------------------------------------------------------

        // 1. User Mapping (Priority 100)
        merchantMappingDao.getMappingForMerchant(merchantName)?.let {
            finalCategory = it.categoryName
            categoryConfidence = 1.0f
            categorySource = TfIdfNaiveBayes.Source.USER_MAPPING
        }

        // 2. High-Priority DB Rules (Priority 80+)
        if (categoryConfidence == 0.0f) {
            val rules = ruleRepository.getActiveRulesList()
            rules.firstOrNull { 
                it.priority >= 80 && 
                (it.transactionType == null || it.transactionType == type.name) &&
                upper.contains(it.keyword.uppercase(Locale.ROOT)) 
            }?.let {
                finalCategory = it.category
                categoryConfidence = 0.95f
                categorySource = TfIdfNaiveBayes.Source.DB_RULE
            }
        }

        // 3. TF-IDF Naive Bayes (Only if highly confident)
        if (categoryConfidence == 0.0f) {
            // Lazy-train if needed (Filter "Others" as per Patch 1)
            if (!tfIdfNaiveBayes.isTrained()) {
                val history = transactionRepository.getRecentTransactions(200).first()
                if (history.isNotEmpty()) tfIdfNaiveBayes.train(history)
            }
            
            val nbResult = tfIdfNaiveBayes.classify(merchantName)
            if (nbResult.confidence >= 0.85f && nbResult.source != TfIdfNaiveBayes.Source.FALLBACK) {
                finalCategory = nbResult.category
                categoryConfidence = nbResult.confidence
                categorySource = nbResult.source
            }
        }

        // 4. Low-Priority DB Rules (Fallback catch-alls)
        if (categoryConfidence == 0.0f) {
            val rules = ruleRepository.getActiveRulesList()
            rules.firstOrNull { 
                it.priority < 80 && 
                (it.transactionType == null || it.transactionType == type.name) &&
                upper.contains(it.keyword.uppercase(Locale.ROOT)) 
            }?.let {
                finalCategory = it.category
                categoryConfidence = 0.60f
                categorySource = TfIdfNaiveBayes.Source.DB_RULE
            }
        }

        // -------------------------------------------------------------
        // PARALLEL TRACK: Welford's Stats Update (Numerical Stability)
        // -------------------------------------------------------------
        val stats = merchantStatsDao.getStats(merchantName)
        val updatedStats = amountAnomalyScorer.updateStats(
            stats, merchantName, finalCategory, amount, timestamp
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
            expectedAmount = anomalyResult.expectedAmount
        )
    }

    /**
     * FIX 3: Strict Gating for merchant_mappings updates.
     */
    fun onCategoryCorrection(merchantName: String, correctCategory: String, wasManual: Boolean) {
        if (!wasManual) {
            android.util.Log.w("ML_ENGINE", "Attempted to auto-save ML guess to merchant_mappings. Blocked.")
            return
        }

        tfIdfNaiveBayes.applyUserCorrection(merchantName, correctCategory)
        
        CoroutineScope(Dispatchers.IO).launch {
            merchantMappingDao.upsertMapping(
                MerchantMappingEntity(
                    merchantName = merchantName, 
                    categoryName = correctCategory,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * PURGE: Clears poisoned database tables (mappings and stats) 
     * to allow the new improved ML logic to take over.
     */
    suspend fun purgePoisonedData() {
        merchantMappingDao.nukeAllMappings()
        merchantStatsDao.nukeAllStats()
    }

    private fun dayOfMonth(epochMillis: Long): Int {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return cal.get(java.util.Calendar.DAY_OF_MONTH)
    }
}
