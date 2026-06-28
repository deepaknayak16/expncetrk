package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.*
import com.example.expncetracker.exptkr.data.db.entity.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface RecurringTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: RecurringTemplateEntity): Long

    @Update
    suspend fun update(template: RecurringTemplateEntity)

    @Query("SELECT * FROM recurring_templates WHERE state = :state")
    fun getTemplatesByState(state: String): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates WHERE cleanMerchantName = :cleanName LIMIT 1")
    suspend fun getTemplateByCleanMerchant(cleanName: String): RecurringTemplateEntity?

    @Query("SELECT * FROM recurring_templates")
    fun getAllTemplates(): Flow<List<RecurringTemplateEntity>>

    @Query("SELECT * FROM recurring_templates")
    suspend fun getAllTemplatesSync(): List<RecurringTemplateEntity>

    @Query("SELECT * FROM recurring_templates WHERE state = 'ACTIVE' AND nextDueDate <= :timestamp")
    suspend fun getDueTemplatesSync(timestamp: Long): List<RecurringTemplateEntity>

    @Query("UPDATE recurring_templates SET nextDueDate = :nextDate, lastDetectedDate = :lastDate WHERE id = :id")
    suspend fun atomicallyBumpDate(id: Long, nextDate: Long, lastDate: Long)

    @Query("UPDATE recurring_templates SET amount = :newAmount WHERE id = :id")
    suspend fun updateExpectedAmount(id: Long, newAmount: BigDecimal)

    @Query("UPDATE recurring_templates SET state = :newState WHERE id = :id")
    suspend fun updateState(id: Long, newState: String)
}
