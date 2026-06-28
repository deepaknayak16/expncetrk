package com.example.expncetracker.exptkr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-merchant running statistics maintained by Welford's online algorithm.
 */
@Entity(tableName = "merchant_stats")
data class MerchantStatsEntity(
    // PK: canonicalized merchant name (trim + uppercase)
    @PrimaryKey val merchantName: String,

    // --- Amount regression head ---
    val amountMean: Double = 0.0,       // Running mean of all transaction amounts
    val amountM2: Double = 0.0,         // Welford M2 accumulator (sum of squared deviations)
    val amountCount: Int = 0,           // Number of samples seen so far

    // --- Recurring regression head ---
    val lastSeenTimestamp: Long = 0L,   // Epoch millis of the most recent occurrence
    val intervalMeanDays: Double = 0.0, // Running mean of inter-transaction day gaps
    val intervalM2: Double = 0.0,       // Welford M2 for intervals
    val intervalCount: Int = 0,         // Number of *intervals* observed (= amountCount - 1)

    // --- Computed helpers (denormalised for query speed) ---
    val category: String = "",          // Most recent category for this merchant
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Derived: population variance (safe for count < 2 → returns 0)
    val amountVariance: Double get() = if (amountCount > 1) amountM2 / amountCount else 0.0
    val amountStdDev: Double get() = Math.sqrt(amountVariance)
    val intervalVariance: Double get() = if (intervalCount > 1) intervalM2 / intervalCount else 0.0
    val intervalStdDev: Double get() = Math.sqrt(intervalVariance)

    // Coefficient of variation: std / mean.  0 = perfectly stable, 1+ = very variable
    val amountCV: Double get() = if (amountMean > 0) amountStdDev / amountMean else 1.0
    val intervalCV: Double get() = if (intervalMeanDays > 0) intervalStdDev / intervalMeanDays else 1.0
}
