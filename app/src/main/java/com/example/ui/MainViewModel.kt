package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DoseNoteApplication
import com.example.data.model.CaregiverLink
import com.example.data.model.DoseItem
import com.example.data.model.Medication
import com.example.data.model.Profile
import com.example.data.model.ProfileRelationship
import com.example.data.model.User
import com.example.data.repository.AdherenceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DoseNoteApplication
    private val userRepository = app.userRepository
    private val medicationRepository = app.medicationRepository
    private val caregiverRepository = app.caregiverRepository

    val currentUser: StateFlow<User?> = userRepository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allProfiles: StateFlow<List<Profile>> = userRepository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCaregiverLinks: StateFlow<List<CaregiverLink>> = caregiverRepository.allCaregiverLinksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProfileId = MutableStateFlow<String?>(null)
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    // Selected date for Adherence tab (midnight millis)
    private val _selectedAdherenceDateMillis = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedAdherenceDateMillis: StateFlow<Long> = _selectedAdherenceDateMillis.asStateFlow()

    // Today's start and end of day millis
    private val _todayStartMillis = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    private val _todayEndMillis = MutableStateFlow(getEndOfDay(System.currentTimeMillis()))

    // Low supply medications for active profile
    private val _lowSupplyMeds = MutableStateFlow<List<Medication>>(emptyList())
    val lowSupplyMeds: StateFlow<List<Medication>> = _lowSupplyMeds.asStateFlow()

    // Dismissed low supply banner IDs for current session
    private val _dismissedLowSupplyIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedLowSupplyIds: StateFlow<Set<String>> = _dismissedLowSupplyIds.asStateFlow()

    // Today's doses for active profile
    private val _todayDoses = MutableStateFlow<List<DoseItem>>(emptyList())
    val todayDoses: StateFlow<List<DoseItem>> = _todayDoses.asStateFlow()

    // Selected date doses for Adherence tab
    private val _adherenceDayDoses = MutableStateFlow<List<DoseItem>>(emptyList())
    val adherenceDayDoses: StateFlow<List<DoseItem>> = _adherenceDayDoses.asStateFlow()

    // 7-day adherence stats for active profile
    private val _adherenceSummary = MutableStateFlow<AdherenceSummary?>(null)
    val adherenceSummary: StateFlow<AdherenceSummary?> = _adherenceSummary.asStateFlow()

    // Paywall BottomSheet state
    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    init {
        // Auto-select first profile when profiles load
        viewModelScope.launch {
            allProfiles.collect { profiles ->
                if (_activeProfileId.value == null && profiles.isNotEmpty()) {
                    _activeProfileId.value = profiles.first().id
                }
            }
        }

        // Observe active profile and date changes to load today's doses and low supply warnings
        viewModelScope.launch {
            combine(_activeProfileId, _todayStartMillis, _todayEndMillis) { profileId, start, end ->
                Triple(profileId, start, end)
            }.collect { (profileId, start, end) ->
                if (profileId != null) {
                    medicationRepository.checkMissedDoses()
                    medicationRepository.getDoseItemsForProfileAndRange(profileId, start, end)
                        .collect { doses ->
                            _todayDoses.value = doses
                        }
                } else {
                    _todayDoses.value = emptyList()
                }
            }
        }

        // Observe low supply medications
        viewModelScope.launch {
            _activeProfileId.collect { profileId ->
                if (profileId != null) {
                    medicationRepository.getLowSupplyMedications(profileId).collect { meds ->
                        _lowSupplyMeds.value = meds
                    }
                }
            }
        }

        // Refresh adherence stats when active profile or tab changes
        viewModelScope.launch {
            _activeProfileId.collect { profileId ->
                if (profileId != null) {
                    refreshAdherence(profileId)
                }
            }
        }

        // Load doses for selected date in Adherence tab
        viewModelScope.launch {
            combine(_activeProfileId, _selectedAdherenceDateMillis) { profileId, dateMillis ->
                Pair(profileId, dateMillis)
            }.collect { (profileId, dateMillis) ->
                if (profileId != null) {
                    val start = getStartOfDay(dateMillis)
                    val end = getEndOfDay(dateMillis)
                    medicationRepository.getDoseItemsForProfileAndRange(profileId, start, end)
                        .collect { doses ->
                            _adherenceDayDoses.value = doses
                        }
                }
            }
        }
    }

    fun setActiveProfile(profileId: String) {
        _activeProfileId.value = profileId
        viewModelScope.launch {
            refreshAdherence(profileId)
        }
    }

    fun setSelectedAdherenceDate(dateMillis: Long) {
        _selectedAdherenceDateMillis.value = getStartOfDay(dateMillis)
    }

    fun refreshAdherence(profileId: String) {
        viewModelScope.launch {
            _adherenceSummary.value = medicationRepository.calculateAdherence(profileId, days = 7)
        }
    }

    fun markDoseTaken(doseLogId: String, medicationId: String) {
        viewModelScope.launch {
            medicationRepository.markDoseTaken(doseLogId, medicationId)
            _activeProfileId.value?.let { refreshAdherence(it) }
        }
    }

    fun markDoseSkipped(doseLogId: String) {
        viewModelScope.launch {
            medicationRepository.markDoseSkipped(doseLogId)
            _activeProfileId.value?.let { refreshAdherence(it) }
        }
    }

    fun snoozeDose(doseLogId: String, medicationId: String, medName: String, doseDesc: String, profileName: String?) {
        medicationRepository.snoozeDose(doseLogId, medicationId, medName, doseDesc, profileName)
    }

    fun dismissLowSupplyBanner(medicationId: String) {
        _dismissedLowSupplyIds.value = _dismissedLowSupplyIds.value + medicationId
    }

    fun openPaywall() {
        _showPaywall.value = true
    }

    fun closePaywall() {
        _showPaywall.value = false
    }

    fun subscribeFamilyPlan() {
        viewModelScope.launch {
            userRepository.setFamilyPlanSubscribed(true)
            _showPaywall.value = false
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            userRepository.updateLanguage(lang)
        }
    }

    fun completeOnboarding(
        phone: String,
        name: String,
        profileName: String,
        relationship: ProfileRelationship,
        language: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val profile = userRepository.completeOnboarding(phone, name, profileName, relationship, language)
            _activeProfileId.value = profile.id
            onSuccess()
        }
    }

    fun addProfile(
        name: String,
        relationship: ProfileRelationship,
        onSuccess: (Profile) -> Unit,
        onPaywallNeeded: () -> Unit
    ) {
        viewModelScope.launch {
            val result = userRepository.addProfile(name, relationship)
            result.onSuccess { profile ->
                _activeProfileId.value = profile.id
                onSuccess(profile)
            }.onFailure { err ->
                if (err.message == "FAMILY_PLAN_REQUIRED") {
                    onPaywallNeeded()
                }
            }
        }
    }

    fun inviteCaregiver(
        profileId: String,
        phone: String,
        name: String?,
        onSuccess: (CaregiverLink) -> Unit,
        onPaywallNeeded: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = caregiverRepository.sendCaregiverInvite(profileId, phone, name)
            result.onSuccess { link ->
                onSuccess(link)
            }.onFailure { err ->
                if (err.message == "FAMILY_PLAN_REQUIRED") {
                    onPaywallNeeded()
                } else {
                    onError(err.message ?: "Failed to invite caregiver")
                }
            }
        }
    }

    fun acceptCaregiverInvite(
        inviteId: String,
        profileName: String,
        ownerPhone: String,
        onSuccess: (Profile) -> Unit
    ) {
        viewModelScope.launch {
            val result = caregiverRepository.acceptCaregiverInvite(inviteId, profileName, ownerPhone)
            result.onSuccess { sharedProfile ->
                _activeProfileId.value = sharedProfile.id
                onSuccess(sharedProfile)
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            userRepository.signOut()
            _activeProfileId.value = null
            onComplete()
        }
    }

    companion object {
        fun getStartOfDay(millis: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        fun getEndOfDay(millis: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
    }
}
