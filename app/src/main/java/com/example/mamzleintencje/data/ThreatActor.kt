package com.example.mamzleintencje.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "threat_actors",
    foreignKeys = [
        ForeignKey(
            entity = IntentRecord::class,
            parentColumns = ["id"],
            childColumns = ["intentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["intentId"]), Index(value = ["packageName"])]
)
data class ThreatActor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val intentId: String,
    val packageName: String,
    val score: Double,
    val timestamp: Long,
    val role: String // "SENDER" or "RECEIVER"
)
