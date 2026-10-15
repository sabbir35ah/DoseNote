package com.example.data.repository

import com.example.data.local.DoseNoteDatabase
import com.example.data.model.Profile
import com.example.data.model.ProfileRelationship
import com.example.data.model.User
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(private val database: DoseNoteDatabase) {

    val currentUserFlow: Flow<User?> = database.userDao().getCurrentUserFlow()
    val allProfilesFlow: Flow<List<Profile>> = database.profileDao().getAllProfilesFlow()

    suspend fun getCurrentUser(): User? = database.userDao().getCurrentUser()

    suspend fun completeOnboarding(
        phone: String,
        userName: String,
        profileName: String,
        relationship: ProfileRelationship,
        language: String
    ): Profile {
        val userId = UUID.randomUUID().toString()
        val user = User(
            id = userId,
            name = userName.ifBlank { profileName },
            phone = phone,
            role = UserRole.SELF,
            language = language,
            isFamilyPlanSubscribed = false
        )
        database.userDao().insertOrUpdate(user)

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            ownerId = userId,
            displayName = profileName.ifBlank { userName },
            relationship = relationship,
            isCaregiverShared = false
        )
        database.profileDao().insertOrUpdate(profile)
        return profile
    }

    suspend fun canCreateProfile(): Boolean {
        val user = getCurrentUser() ?: return false
        val ownedProfilesCount = database.profileDao().getOwnedProfileCount()
        // Free tier allows exactly 1 profile; Family tier allows unlimited profiles
        return user.isFamilyPlanSubscribed || ownedProfilesCount < 1
    }

    suspend fun addProfile(displayName: String, relationship: ProfileRelationship): Result<Profile> {
        val user = getCurrentUser() ?: return Result.failure(IllegalStateException("User not logged in"))
        if (!canCreateProfile()) {
            return Result.failure(IllegalStateException("FAMILY_PLAN_REQUIRED"))
        }

        val profile = Profile(
            id = UUID.randomUUID().toString(),
            ownerId = user.id,
            displayName = displayName,
            relationship = relationship,
            isCaregiverShared = false
        )
        database.profileDao().insertOrUpdate(profile)
        return Result.success(profile)
    }

    suspend fun setFamilyPlanSubscribed(subscribed: Boolean) {
        val user = getCurrentUser() ?: return
        database.userDao().setSubscription(user.id, subscribed)
    }

    suspend fun updateLanguage(lang: String) {
        val user = getCurrentUser() ?: return
        database.userDao().updateLanguage(user.id, lang)
    }

    suspend fun getOwnedProfileCount(): Int {
        return database.profileDao().getOwnedProfileCount()
    }

    suspend fun signOut() {
        database.userDao().clear()
        database.profileDao().clear()
    }
}
