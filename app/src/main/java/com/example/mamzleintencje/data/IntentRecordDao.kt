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

    @Query("""
        SELECT * FROM intent_records 
        WHERE 
            (:searchQuery = '' 
                OR action LIKE '%' || :searchQuery || '%' 
                OR callerPackage LIKE '%' || :searchQuery || '%'
                OR deliveredReceivers LIKE '%' || :searchQuery || '%'
                OR skippedReceivers LIKE '%' || :searchQuery || '%'
                OR extrasDump LIKE '%' || :searchQuery || '%'
                OR requiredPermissions LIKE '%' || :searchQuery || '%'
                OR cvssVector LIKE '%' || :searchQuery || '%'
                OR skipReasons LIKE '%' || :searchQuery || '%'
                OR deliveryStatus LIKE '%' || :searchQuery || '%'
            )
            AND cvssBaseScore >= :minCvss
            AND (:hideSystemApps = 0 OR (callerPackage != 'system_server' AND callerPackage NOT LIKE 'com.android%' AND callerPackage NOT LIKE 'android%'))
            AND (:hasExtras = 0 OR extrasSize > 0)
            AND (:useStatusFilter = 0 OR deliveryStatus IN (:statusFilters))
        ORDER BY timestamp DESC
    """)
    fun getFilteredRecords(
        searchQuery: String,
        minCvss: Double,
        hideSystemApps: Boolean,
        hasExtras: Boolean,
        useStatusFilter: Boolean,
        statusFilters: List<String>
    ): Flow<List<IntentRecord>>

    @Query("DELETE FROM intent_records")
    suspend fun deleteAll(): Int

}