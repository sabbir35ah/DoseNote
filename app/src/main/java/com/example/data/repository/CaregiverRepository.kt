package com.example.data.repository

import com.example.data.local.DoseNoteDatabase
import com.example.data.model.CaregiverLink
import com.example.data.model.CaregiverLinkStatus
import com.example.data.model.CaregiverPermission
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.data.model.Profile
import com.example.data.model.ProfileRelationship
import com.example.data.model.ScheduleType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class CaregiverRepository(
    private val database: DoseNoteDatabase,
    private val userRepository: UserRepository
) {

    val allCaregiverLinksFlow: Flow<List<CaregiverLink>> = database.caregiverLinkDao().getAllLinksFlow()

    fun getLinksForProfile(profileId: String): Flow<List<CaregiverLink>> {
        return database.caregiverLinkDao().getLinksForProfileFlow(profileId)
    }

    suspend fun sendCaregiverInvite(
        profileId: String,
        caregiverPhone: String,
        caregiverName: String?
    ): Result<CaregiverLink> = withContext(Dispatchers.IO) {
        val user = userRepository.getCurrentUser() ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

        // Tier gating: Caregiver sharing requires Family Plan
        if (!user.isFamilyPlanSubscribed) {
            return@withContext Result.failure(IllegalStateException("FAMILY_PLAN_REQUIRED"))
        }

        val inviteId = UUID.randomUUID().toString()
        val link = CaregiverLink(
            id = inviteId,
            profileId = profileId,
            caregiverUserId = "caregiver_${caregiverPhone.filter { it.isDigit() }}",
            caregiverPhone = caregiverPhone,
            caregiverName = caregiverName,
            permission = CaregiverPermission.READ_ONLY,
            status = CaregiverLinkStatus.PENDING,
            invitedAt = System.currentTimeMillis()
        )

        database.caregiverLinkDao().insertOrUpdate(link)

        // Sync invite to Firestore if Firebase is configured
        try {
            val firestore = FirebaseFirestore.getInstance()
            val inviteDoc = hashMapOf(
                "inviteId" to inviteId,
                "profileId" to profileId,
                "ownerPhone" to user.phone,
                "ownerName" to user.name,
                "caregiverPhone" to caregiverPhone,
                "status" to "PENDING",
                "permission" to "READ_ONLY",
                "invitedAt" to link.invitedAt
            )
            firestore.collection("caregiver_invites").document(inviteId).set(inviteDoc)
        } catch (_: Exception) {
            // Local fallback if Firebase not configured or offline
        }

        Result.success(link)
    }

    suspend fun acceptCaregiverInvite(
        inviteId: String,
        profileDisplayName: String,
        ownerPhone: String
    ): Result<Profile> = withContext(Dispatchers.IO) {
        val user = userRepository.getCurrentUser() ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

        val sharedProfileId = "shared_${UUID.randomUUID().toString().take(8)}"
        val sharedProfile = Profile(
            id = sharedProfileId,
            ownerId = ownerPhone,
            displayName = "$profileDisplayName (Shared)",
            relationship = ProfileRelationship.PARENT,
            isCaregiverShared = true
        )
        database.profileDao().insertOrUpdate(sharedProfile)

        // Seed sample shared medications and status for read-only viewing
        val sampleMed = Medication(
            id = "med_${UUID.randomUUID().toString().take(8)}",
            profileId = sharedProfileId,
            name = "Seclo 20",
            doseDescription = "1 capsule before breakfast",
            scheduleType = ScheduleType.FIXED_TIMES,
            times = listOf("08:00", "20:00"),
            startDate = System.currentTimeMillis() - (86400000L * 3),
            remainingSupply = 4,
            lowSupplyThreshold = 5,
            hasNotifiedLowSupply = true,
            isActive = true
        )
        database.medicationDao().insertOrUpdate(sampleMed)

        val now = System.currentTimeMillis()
        val morningCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
        }
        val nightCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 20)
            set(java.util.Calendar.MINUTE, 0)
        }

        database.doseLogDao().insertOrUpdate(
            DoseLog(
                id = UUID.randomUUID().toString(),
                medicationId = sampleMed.id,
                profileId = sharedProfileId,
                scheduledAt = morningCal.timeInMillis,
                status = DoseStatus.TAKEN,
                respondedAt = morningCal.timeInMillis + 300000
            )
        )
        database.doseLogDao().insertOrUpdate(
            DoseLog(
                id = UUID.randomUUID().toString(),
                medicationId = sampleMed.id,
                profileId = sharedProfileId,
                scheduledAt = nightCal.timeInMillis,
                status = DoseStatus.PENDING
            )
        )

        // Update link in local DB
        val existingLink = database.caregiverLinkDao().getLinkById(inviteId)
        if (existingLink != null) {
            database.caregiverLinkDao().update(existingLink.copy(status = CaregiverLinkStatus.ACCEPTED))
        }

        Result.success(sharedProfile)
    }

    suspend fun syncSharedProfileDoseStatus(
        profileId: String,
        medicationId: String,
        status: DoseStatus,
        remainingSupply: Int?
    ) = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val syncData = hashMapOf(
                "profileId" to profileId,
                "medicationId" to medicationId,
                "status" to status.name,
                "remainingSupply" to remainingSupply,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("shared_profile_status")
                .document("${profileId}_${medicationId}")
                .set(syncData)
        } catch (_: Exception) {
            // Local fallback
        }
    }
}
