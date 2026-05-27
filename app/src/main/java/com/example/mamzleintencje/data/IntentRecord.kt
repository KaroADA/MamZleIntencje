package com.example.mamzleintencje.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intent_records")
data class IntentRecord(
    @PrimaryKey
    val id: String,

    val timestamp: Long,
    val action: String?,
    val callerPackage: String?,
    val targetComponent: String?,
    val intentType: IntentType,
    val extrasSize: Int,
    val extrasDump: String?,

    val dispatchTime: Long? = null,
    val finishTime: Long? = null,

    val cvssVector: String = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N",
    val cvssBaseScore: Double = 0.0
)

enum class IntentType {
    EXPLICIT,
    IMPLICIT,
    BROADCAST,
    UNKNOWN
}