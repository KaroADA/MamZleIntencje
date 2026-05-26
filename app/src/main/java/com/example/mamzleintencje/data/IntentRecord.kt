package com.example.mamzleintencje.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intent_records")
data class IntentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long,
    val action: String?,
    val callerPackage: String?,
    val targetComponent: String?,
    val intentType: IntentType,
    val extrasSize: Int,
    val extrasDump: String?,

    val dispatchTime: Long? = null,
    val finishTime: Long? = null,

    var threatScore: Int = 0
)

enum class IntentType {
    EXPLICIT,
    IMPLICIT,
    BROADCAST,
    UNKNOWN
}