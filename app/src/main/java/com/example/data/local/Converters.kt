package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CaregiverLinkStatus
import com.example.data.model.CaregiverPermission
import com.example.data.model.DoseStatus
import com.example.data.model.ProfileRelationship
import com.example.data.model.ScheduleType
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrEmpty()) return null
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (_: Exception) {
        UserRole.SELF
    }

    @TypeConverter
    fun fromProfileRelationship(value: ProfileRelationship): String = value.name

    @TypeConverter
    fun toProfileRelationship(value: String): ProfileRelationship = try {
        ProfileRelationship.valueOf(value)
    } catch (_: Exception) {
        ProfileRelationship.SELF
    }

    @TypeConverter
    fun fromScheduleType(value: ScheduleType): String = value.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = try {
        ScheduleType.valueOf(value)
    } catch (_: Exception) {
        ScheduleType.FIXED_TIMES
    }

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = try {
        DoseStatus.valueOf(value)
    } catch (_: Exception) {
        DoseStatus.PENDING
    }

    @TypeConverter
    fun fromCaregiverPermission(value: CaregiverPermission): String = value.name

    @TypeConverter
    fun toCaregiverPermission(value: String): CaregiverPermission = try {
        CaregiverPermission.valueOf(value)
    } catch (_: Exception) {
        CaregiverPermission.READ_ONLY
    }

    @TypeConverter
    fun fromCaregiverLinkStatus(value: CaregiverLinkStatus): String = value.name

    @TypeConverter
    fun toCaregiverLinkStatus(value: String): CaregiverLinkStatus = try {
        CaregiverLinkStatus.valueOf(value)
    } catch (_: Exception) {
        CaregiverLinkStatus.PENDING
    }
}
