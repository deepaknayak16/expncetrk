package com.example.expncetracker.exptkr.core.ml

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.expncetracker.exptkr.core.common.NB_ML_MODEL_KEY
import com.example.expncetracker.exptkr.core.common.dataStore
import com.example.expncetracker.exptkr.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln

/**
 * TF-IDF Weighted Naive Bayes classifier for transaction category detection.
 * 
 * FIX 1: Added @Synchronized to all state-mutating and state-reading methods to prevent race conditions.
 * FIX BUG-ML-04: Added DataStore persistence for trained model state.
 */
@Singleton
class TfIdfNaiveBayes @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Serializable
    data class ModelSnapshot(
        val logLikelihoods: Map<String, Map<String, Double>>,
        val idfWeights: Map<String, Double>,
        val logPriors: Map<String, Double>,
        val allCategories: List<String>,
        val trainedOnCount: Int
    )

    data class ClassificationResult(
        val category: String,
        val confidence: Float,
        val runnerUpCategory: String?,
        val runnerUpConfidence: Float,
        val source: Source
    )

    enum class Source { USER_MAPPING, DB_RULE, HISTORY_EXACT, TFIDF_NB, FALLBACK }

    private var logPriors: Map<String, Double> = emptyMap()
    private var logLikelihoods: Map<String, Map<String, Double>> = emptyMap()
    private var idfWeights: Map<String, Double> = emptyMap()
    private var knownVocab: Set<String> = emptySet()
    private var allCategories: List<String> = emptyList()
    private var trainedOnCount: Int = 0

    init {
        // Load saved model on init
        CoroutineScope(Dispatchers.IO).launch {
            val saved = context.dataStore.data.first()[NB_ML_MODEL_KEY]
            saved?.let { json ->
                runCatching {
                    val snapshot = Json.decodeFromString<ModelSnapshot>(json)
                    logLikelihoods = snapshot.logLikelihoods
                    idfWeights = snapshot.idfWeights
                    logPriors = snapshot.logPriors
                    allCategories = snapshot.allCategories
                    trainedOnCount = snapshot.trainedOnCount
                    knownVocab = idfWeights.keys
                }
            }
        }
    }

    private fun persistModel() {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                val snapshot = ModelSnapshot(
                    logLikelihoods, idfWeights, logPriors, allCategories, trainedOnCount
                )
                prefs[NB_ML_MODEL_KEY] = Json.encodeToString(snapshot)
            }
        }
    }

    @Synchronized
    fun train(history: List<Transaction>) {
        // ... (filtering logic) ...
        val cleanHistory = history.filter {
            it.categoryName != "Others" &&
            it.merchant.isNotBlank() &&
            !it.merchant.contains("$")
        }

        if (cleanHistory.isEmpty()) {
            trainedOnCount = 0
            return
        }

        val totalDocs = cleanHistory.size.toDouble()
        allCategories = cleanHistory.map { it.categoryName }.distinct()

        val categoryCounts = cleanHistory.groupingBy { it.categoryName }.eachCount()
        val tokenCountsByCategory = mutableMapOf<String, MutableMap<String, Int>>()
        val docFrequency = mutableMapOf<String, Int>()

        cleanHistory.forEach { tx ->
            val tokens = allTokens(tx.merchant)
            val uniqueTokens = tokens.toSet()
            uniqueTokens.forEach { tok ->
                docFrequency[tok] = (docFrequency[tok] ?: 0) + 1
            }
            tokens.forEach { tok ->
                tokenCountsByCategory
                    .getOrPut(tx.categoryName) { mutableMapOf() }
                    .let { it[tok] = (it[tok] ?: 0) + 1 }
            }
        }

        val vocabSize = docFrequency.size
        idfWeights = docFrequency.mapValues { (_, df) ->
            ln(1.0 + totalDocs / (1.0 + df))
        }
        knownVocab = docFrequency.keys

        val builtLikelihoods = mutableMapOf<String, Map<String, Double>>()
        allCategories.forEach { cat ->
            val tokenCounts = tokenCountsByCategory[cat] ?: emptyMap()
            val totalTokensInCat = tokenCounts.values.sum().toDouble()
            val catLikelihoods = mutableMapOf<String, Double>()

            docFrequency.keys.forEach { tok ->
                val count = (tokenCounts[tok] ?: 0) + 1
                catLikelihoods[tok] = ln(count.toDouble() / (totalTokensInCat + vocabSize))
            }
            builtLikelihoods[cat] = catLikelihoods
        }
        logLikelihoods = builtLikelihoods

        logPriors = allCategories.associateWith { cat ->
            ln(((categoryCounts[cat] ?: 0) + 1.0) / (totalDocs + allCategories.size))
        }

        trainedOnCount = cleanHistory.size
        
        persistModel()
    }

    @Synchronized
    fun classify(merchantName: String): ClassificationResult {
        // ... (existing classify logic) ...
        if (logPriors.isEmpty() || allCategories.isEmpty()) {
            return ClassificationResult("Others", 0f, null, 0f, Source.FALLBACK)
        }

        val tokens = allTokens(merchantName)
        if (tokens.isEmpty()) {
            return ClassificationResult("Others", 0.1f, null, 0f, Source.FALLBACK)
        }

        // FIX BUG-ML-03: Track how many tokens we actually recognize using knownVocab
        var knownTokenCount = 0
        tokens.forEach { tok -> if (tok in knownVocab) knownTokenCount++ }

        val tf = tokens.groupingBy { it }.eachCount()
        val maxTf = tf.values.maxOrNull()?.toDouble() ?: 1.0

        val scores = mutableMapOf<String, Double>()
        allCategories.forEach { cat ->
            var score = logPriors[cat] ?: -10.0
            val likelihoods = logLikelihoods[cat] ?: emptyMap()

            tokens.forEach { tok ->
                val normTf = (tf[tok] ?: 0) / maxTf
                val idf = idfWeights[tok] ?: 3.0
                
                if (likelihoods.containsKey(tok)) {
                    val logLik = likelihoods[tok] ?: -10.0
                    score += (normTf * idf) * logLik
                } else {
                    score += (normTf * idf) * -15.0 // Unknown token penalty
                }
            }
            scores[cat] = score
        }

        // Softmax to get calibrated probabilities
        val sortedEntries = scores.entries.sortedByDescending { it.value }
        val maxScore = sortedEntries.first().value
        val expScores = sortedEntries.map { (cat, s) -> cat to exp(s - maxScore) }
        val sumExp = expScores.sumOf { it.second }
        val probabilities = expScores.map { (cat, e) -> cat to (e / sumExp) }

        val bestCat = probabilities[0].first
        val bestConf = probabilities[0].second.toFloat()

        // CRITICAL FIX BUG-ML-03/01: If we recognize fewer than 2 tokens, or less than 30% of tokens,
        // we are "ignorant" and should not be confident.
        val knownRatio = knownTokenCount.toFloat() / tokens.size
        if (knownTokenCount < 2 || (tokens.size >= 3 && knownRatio < 0.3f)) {
            return ClassificationResult("Others", 0.2f, null, 0f, Source.FALLBACK)
        }

        val secondCat = probabilities.getOrNull(1)?.first
        val secondConf = probabilities.getOrNull(1)?.second?.toFloat() ?: 0f

        return ClassificationResult(
            category = bestCat,
            confidence = bestConf,
            runnerUpCategory = secondCat,
            runnerUpConfidence = secondConf,
            source = Source.TFIDF_NB
        )
    }

    @Synchronized
    fun applyUserCorrection(merchantName: String, correctCategory: String) {
        // Clearing trainedOnCount to force retrain in HybridMlEngine
        trainedOnCount = 0 
        persistModel()
    }

    @Synchronized
    fun reset() {
        logLikelihoods = emptyMap()
        idfWeights = emptyMap()
        knownVocab = emptySet()
        allCategories = emptyList()
        trainedOnCount = 0
        logPriors = emptyMap()
        persistModel()
    }

    fun allTokens(text: String): List<String> {
        val clean = text.lowercase(Locale.ROOT).trim()
        val words = clean.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }
        val charNgrams = (3..4).flatMap { n ->
            val noSpaces = clean.filter { it.isLetterOrDigit() }
            if (noSpaces.length < n) emptyList()
            else (0..noSpaces.length - n).map { noSpaces.substring(it, it + n) }
        }
        return (words + charNgrams).ifEmpty { listOf("UNKNOWN") }
    }

    @Synchronized
    fun isTrained(): Boolean = trainedOnCount > 0
}
