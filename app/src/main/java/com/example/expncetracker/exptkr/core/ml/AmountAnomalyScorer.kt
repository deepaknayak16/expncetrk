package com.example.expncetracker.exptkr.core.ml

import com.example.expncetracker.exptkr.data.db.entity.MerchantStatsEntity
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Amount anomaly detection and expected-amount prediction.
 */
@Singleton
class AmountAnomalyScorer @Inject constructor() {

    enum class AnomalyLevel {
        NORMAL,     // |z| < 1.5
        UNUSUAL,    // 1.5 ≤ |z| < 2.5
        ANOMALY,    // 2.5 ≤ |z| < 4.0
        ALERT       // |z| ≥ 4.0
    }

    data class ScoringResult(
        val zScore: Float,
        val level: AnomalyLevel,
        val expectedAmount: Double?,
        val amountMean: Double,
        val amountStdDev: Double
    )

    fun score(amount: BigDecimal, stats: MerchantStatsEntity?): ScoringResult {
        if (stats == null || stats.amountCount < 3) {
            return ScoringResult(
                zScore = 0f,
                level = AnomalyLevel.NORMAL,
                expectedAmount = null,
                amountMean = amount.toDouble(),
                amountStdDev = 0.0
            )
        }

        val mean = stats.amountMean
        val stdDev = stats.amountStdDev

        if (stdDev < 1.0) {
            val deviation = abs(amount.toDouble() - mean) / mean
            val level = if (deviation > 0.10) AnomalyLevel.UNUSUAL else AnomalyLevel.NORMAL
            return ScoringResult(
                zScore = deviation.toFloat() * 5f,
                level = level,
                expectedAmount = mean,
                amountMean = mean,
                amountStdDev = stdDev
            )
        }

        val z = ((amount.toDouble() - mean) / stdDev).toFloat()
        val level = when (abs(z)) {
            in 0f..1.5f  -> AnomalyLevel.NORMAL
            in 1.5f..2.5f -> AnomalyLevel.UNUSUAL
            in 2.5f..4.0f -> AnomalyLevel.ANOMALY
            else          -> AnomalyLevel.ALERT
        }

        return ScoringResult(
            zScore = z,
            level = level,
            expectedAmount = if (stats.amountCount >= 3) mean else null,
            amountMean = mean,
            amountStdDev = stdDev
        )
    }

    fun toConfidenceScore(level: AnomalyLevel): Float = when (level) {
        AnomalyLevel.NORMAL  -> 0.95f
        AnomalyLevel.UNUSUAL -> 0.75f
        AnomalyLevel.ANOMALY -> 0.50f
        AnomalyLevel.ALERT   -> 0.30f
    }

    fun toParsingStatus(level: AnomalyLevel): String = when (level) {
        AnomalyLevel.NORMAL, AnomalyLevel.UNUSUAL -> "COMPLETE"
        AnomalyLevel.ANOMALY, AnomalyLevel.ALERT  -> "NEEDS_REVIEW"
    }

    fun updateStats(
        existing: MerchantStatsEntity?,
        merchantName: String,
        category: String,
        newAmount: BigDecimal,
        newTimestamp: Long
    ): MerchantStatsEntity {
        val amount = newAmount.toDouble()

        if (existing == null) {
            return MerchantStatsEntity(
                merchantName = merchantName,
                amountMean = amount,
                amountM2 = 0.0,
                amountCount = 1,
                lastSeenTimestamp = newTimestamp,
                intervalMeanDays = 0.0,
                intervalM2 = 0.0,
                intervalCount = 0,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        }

        val n = existing.amountCount + 1
        val delta = amount - existing.amountMean
        val newMean = existing.amountMean + delta / n
        val delta2 = amount - newMean
        val newM2 = existing.amountM2 + delta * delta2

        val intervalDays = if (existing.lastSeenTimestamp > 0L) {
            (newTimestamp - existing.lastSeenTimestamp) / (1000.0 * 60 * 60 * 24)
        } else null

        val (newIntervalMean, newIntervalM2, newIntervalCount) = if (intervalDays != null && intervalDays > 0.0) {
            val iN = existing.intervalCount + 1
            val iDelta = intervalDays - existing.intervalMeanDays
            val iNewMean = existing.intervalMeanDays + iDelta / iN
            val iDelta2 = intervalDays - iNewMean
            val iNewM2 = existing.intervalM2 + iDelta * iDelta2
            Triple(iNewMean, iNewM2, iN)
        } else {
            Triple(existing.intervalMeanDays, existing.intervalM2, existing.intervalCount)
        }

        return existing.copy(
            amountMean = newMean,
            amountM2 = newM2,
            amountCount = n,
            lastSeenTimestamp = newTimestamp,
            intervalMeanDays = newIntervalMean,
            intervalM2 = newIntervalM2,
            intervalCount = newIntervalCount,
            category = category,
            updatedAt = System.currentTimeMillis()
        )
    }
}
