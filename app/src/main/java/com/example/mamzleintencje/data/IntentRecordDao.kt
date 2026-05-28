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
    @Query("SELECT * FROM intent_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): IntentRecord?
    @Query("UPDATE intent_records SET wasSeen = 1 WHERE id = :id")
    suspend fun markAsSeen(id: String): Int

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
            AND (:requiresPermission IS NULL OR (:requiresPermission = 1 AND requiredPermissions IS NOT NULL AND requiredPermissions != '') OR (:requiresPermission = 0 AND (requiredPermissions IS NULL OR requiredPermissions = '')))
        ORDER BY timestamp DESC
    """)
    fun getFilteredRecords(
        searchQuery: String,
        minCvss: Double,
        hideSystemApps: Boolean,
        hasExtras: Boolean,
        useStatusFilter: Boolean,
        statusFilters: List<String>,
        requiresPermission: Boolean?
    ): Flow<List<IntentRecord>>

    @Query("DELETE FROM intent_records")
    suspend fun deleteAll(): Int

    @Query("SELECT MAX(cvssBaseScore) FROM intent_records")
    fun getMaxCvssScore(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM intent_records WHERE cvssBaseScore >= 7.0")
    fun getCriticalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM intent_records WHERE cvssBaseScore >= 3.0 AND cvssBaseScore < 7.0")
    fun getMediumCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM intent_records WHERE cvssBaseScore < 3.0")
    fun getLowCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM intent_records")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM intent_records WHERE cvssBaseScore > 0")
    fun getSuspiciousCount(): Flow<Int>

    data class PackageRisk(
        val callerPackage: String?,
        val maxScore: Double,
        val intentCount: Int
    )

    @Query("""
        SELECT callerPackage, MAX(cvssBaseScore) as maxScore, COUNT(*) as intentCount 
        FROM intent_records 
        GROUP BY callerPackage 
        ORDER BY maxScore DESC 
        LIMIT 5
    """)
    fun getTopDangerousPackages(): Flow<List<PackageRisk>>

    @Query("SELECT * FROM intent_records WHERE cvssBaseScore > 0 ORDER BY timestamp DESC LIMIT 3")
    fun getRecentSuspiciousIntents(): Flow<List<IntentRecord>>
}
