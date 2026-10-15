package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CaregiverPermission {
    READ_ONLY
}

enum class CaregiverLinkStatus {
    PENDING,
    ACCEPTED
}

@Entity(tableName = "caregiver_links")
data class CaregiverLink(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val caregiverUserId: String,
    val caregiverPhone: String,
    val caregiverName: String? = null,
    val permission: CaregiverPermission = CaregiverPermission.READ_ONLY,
    val status: CaregiverLinkStatus = CaregiverLinkStatus.PENDING,
    val invitedAt: Long = System.currentTimeMillis()
)
