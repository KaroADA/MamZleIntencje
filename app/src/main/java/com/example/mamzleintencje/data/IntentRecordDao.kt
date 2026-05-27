package com.example.mamzleintencje.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IntentRecordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: IntentRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(records: List<IntentRecord>): List<Long>

    @Query("SELECT * FROM intent_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<IntentRecord>>

    @Query("DELETE FROM intent_records")
    suspend fun deleteAll(): Int
}