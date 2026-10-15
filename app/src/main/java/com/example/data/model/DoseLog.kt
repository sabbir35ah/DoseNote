package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED
}

@Entity(
    tableName = "dose_logs",
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["scheduledAt"]),
        Index(value = ["profileId", "scheduledAt"])
    ]
)
data class DoseLog(
    @PrimaryKey
    val id: String,
    val medicationId: String,
    val profileId: String,
    val scheduledAt: Long, // Epoch millis exact scheduled reminder timestamp
    val status: DoseStatus = DoseStatus.PENDING,
    val respondedAt: Long? = null,
    val note: String? = null
)
