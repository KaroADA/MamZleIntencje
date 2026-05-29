package com.example.mamzleintencje.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intent_records")
data class IntentRecord(
    @PrimaryKey
    val id: String,                  // SHA-256 hash of unique properties to prevent duplicates
    val wasSeen: Boolean = false,

    val timestamp: Long,
    val action: String?,
    val callerPackage: String?,
    val callerUid: Int?,

    val requiredPermissions: String?,

    val extrasSize: Int,
    val extrasDump: String?,

    val dispatchTime: Long? = null,
    val finishTime: Long? = null,

    val deliveryStatus: String?,     // "DELIVERED", "PARTIALLY_SKIPPED", "SKIPPED", or "DEFERRED"
    val totalReceiverCount: Int,     // Total scheduled receivers (from terminalCount)
    val deliveredReceivers: String?, // Comma-separated list of target packages that successfully received it
    val skippedReceivers: String?,   // Comma-separated list of target packages that were skipped/blocked
    val skipReasons: String?,        // Semicolon-separated list of why skipped targets were skipped

    val cvssVector: String = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N",
    val cvssBaseScore: Double = 0.0,
    val riskReasons: String? = null
)

enum class IntentType {
    EXPLICIT,
    IMPLICIT,
    BROADCAST,
    UNKNOWN
}