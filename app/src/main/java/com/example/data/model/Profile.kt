package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProfileRelationship {
    SELF,
    PARENT,
    CHILD,
    OTHER
}

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val displayName: String,
    val relationship: ProfileRelationship = ProfileRelationship.SELF,
    val photoUri: String? = null,
    val isCaregiverShared: Boolean = false // If this profile is viewed by current user via caregiver access
)
