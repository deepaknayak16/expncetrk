package com.example.expncetracker.exptkr.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expncetracker.exptkr.data.db.entity.RawSmsEntity

@Dao
interface RawSmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRawSmsList(smsList: List<RawSmsEntity>)

    @Query("SELECT * FROM raw_sms ORDER BY timestamp DESC")
    suspend fun getAllRawSms(): List<RawSmsEntity>
}
