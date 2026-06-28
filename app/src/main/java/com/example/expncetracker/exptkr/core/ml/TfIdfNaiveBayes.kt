package com.example.expncetracker.exptkr.core.ml

import com.example.expncetracker.exptkr.domain.model.Transaction
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln

/**
 * TF-IDF Weighted Naive Bayes classifier for transaction category detection.
 * 
 * FIX 1: Added @Synchronized to all state-mutating and state-reading methods to prevent race conditions.
 */
@Singleton
class TfIdfNaiveBayes @Inject constructor() {

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
    private var allCategories: List<String> = emptyList()
    private var trainedOnCount: Int = 0

    @Synchronized
    fun train(history: List<Transaction>) {
        // CRITICAL FIX: Do not let the model learn the "Others" category.
        // "Others" is an absence of signal, not a category.
        // Also filter out parser bugs containing string templates.
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

        trainedOnCount = history.size
    }

    @Synchronized
    fun classify(merchantName: String): ClassificationResult {
        if (logPriors.isEmpty() || allCategories.isEmpty()) {
            return ClassificationResult("Others", 0f, null, 0f, Source.FALLBACK)
        }

        val tokens = allTokens(merchantName)
        if (tokens.isEmpty()) {
            return ClassificationResult("Others", 0.1f, null, 0f, Source.FALLBACK)
        }

        // Track how many tokens we actually recognize
        var knownTokenCount = 0

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
                    if (cat == allCategories[0]) knownTokenCount++ 
                    val logLik = likelihoods[tok] ?: -10.0
                    score += (normTf * idf) * logLik
                } else {
                    score += (normTf * idf) * -15.0 // Unknown token penalty
                }
            }
            scores[cat] = score
        }

        // Softmax
        val sortedEntries = scores.entries.sortedByDescending { it.value }
        val maxScore = sortedEntries.first().value
        val expScores = sortedEntries.map { (cat, s) -> cat to exp(s - maxScore) }
        val sumExp = expScores.sumOf { it.second }
        val probabilities = expScores.map { (cat, e) -> cat to (e / sumExp) }

        val bestCat = probabilities[0].first
        val bestConf = probabilities[0].second.toFloat()

        // CRITICAL FIX: If we recognize fewer than 2 tokens, or less than 30% of tokens,
        // we are "ignorant" and should not be confident.
        val knownRatio = knownTokenCount.toFloat() / tokens.size
        if (knownTokenCount < 2 || knownRatio < 0.3f) {
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
        if (!allCategories.contains(correctCategory)) return
        val tokens = allTokens(merchantName)
        val mutableLikelihoods = logLikelihoods.toMutableMap()
        val catMap = mutableLikelihoods[correctCategory]?.toMutableMap() ?: mutableMapOf()

        tokens.forEach { tok ->
            val currentProb = exp(catMap[tok] ?: -10.0)
            catMap[tok] = ln(currentProb + 0.1)
        }
        mutableLikelihoods[correctCategory] = catMap
        logLikelihoods = mutableLikelihoods
    }

    fun allTokens(text: String): List<String> {
        val clean = text.lowercase(Locale.ROOT).trim()
        val words = clean.split(Regex("[^a-z0-9]+")).filter { it.length > 1 }
        val charNgrams = (3..4).flatMap { n ->
            val noSpaces = clean.filter { it.isLetterOrDigit() }
            if (noSpaces.length < n) emptyList()
            else (0..noSpaces.length - n).map { noSpaces.substring(it, it + n) }
        }
        return words + charNgrams
    }

    @Synchronized
    fun isTrained(): Boolean = trainedOnCount > 0
}
