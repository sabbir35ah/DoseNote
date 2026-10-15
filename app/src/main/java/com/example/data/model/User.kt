package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    SELF,
    CAREGIVER
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole = UserRole.SELF,
    val language: String = "en", // "en" or "bn"
    val isFamilyPlanSubscribed: Boolean = false
)
