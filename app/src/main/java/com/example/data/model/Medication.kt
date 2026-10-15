package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScheduleType {
    FIXED_TIMES,
    SPECIFIC_DAYS
}

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val doseDescription: String,
    val scheduleType: ScheduleType = ScheduleType.FIXED_TIMES,
    val times: List<String>, // Formatted as "HH:mm" e.g. ["08:00", "20:00"]
    val daysOfWeek: List<Int>? = null, // Calendar days: 1=Sunday, 2=Monday, ..., 7=Saturday
    val startDate: Long, // Epoch millis start of day
    val endDate: Long? = null, // Epoch millis end of day (optional)
    val remainingSupply: Int? = null,
    val lowSupplyThreshold: Int? = 5,
    val hasNotifiedLowSupply: Boolean = false,
    val isActive: Boolean = true
)
