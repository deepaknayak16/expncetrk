package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expncetracker.exptkr.data.db.entity.SmsQuarantineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsQuarantineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sms: SmsQuarantineEntity)

    @Query("SELECT * FROM sms_quarantine ORDER BY timestamp DESC")
    fun getAllQuarantinedSms(): Flow<List<SmsQuarantineEntity>>

    @Query("DELETE FROM sms_quarantine WHERE smsId = :smsId")
    suspend fun deleteById(smsId: String)
}
