package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.expncetracker.exptkr.data.db.entity.MerchantStatsEntity

@Dao
interface MerchantStatsDao {

    @Query("SELECT * FROM merchant_stats WHERE merchantName = :name COLLATE NOCASE")
    suspend fun getStats(name: String): MerchantStatsEntity?

    /**
     * Upsert: INSERT OR REPLACE handles both first-seen and subsequent updates.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: MerchantStatsEntity)

    /** Returns all merchants that have been seen at least [minCount] times. */
    @Query("""
        SELECT * FROM merchant_stats
        WHERE amountCount >= :minCount
        ORDER BY intervalMeanDays ASC
    """)
    fun getMerchantsWithMinOccurrences(minCount: Int = 2): Flow<List<MerchantStatsEntity>>

    /** Fetch the top N most-recently-updated stats rows. */
    @Query("SELECT * FROM merchant_stats ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentStats(limit: Int = 200): List<MerchantStatsEntity>

    @Query("DELETE FROM merchant_stats")
    suspend fun nukeAllStats()
}
