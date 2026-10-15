package com.example.ui.medication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DoseNoteApplication
import com.example.data.model.Medication
import com.example.data.model.MedicineSuggestion
import com.example.data.model.ScheduleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AddEditMedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DoseNoteApplication
    private val catalogRepo = app.medicineCatalogRepository
    private val medRepo = app.medicationRepository

    private var editingMedicationId: String? = null

    private val _medName = MutableStateFlow("")
    val medName: StateFlow<String> = _medName.asStateFlow()

    private val _doseDesc = MutableStateFlow("")
    val doseDesc: StateFlow<String> = _doseDesc.asStateFlow()

    private val _scheduleType = MutableStateFlow(ScheduleType.FIXED_TIMES)
    val scheduleType: StateFlow<ScheduleType> = _scheduleType.asStateFlow()

    private val _times = MutableStateFlow(listOf("08:00", "20:00"))
    val times: StateFlow<List<String>> = _times.asStateFlow()

    private val _selectedDays = MutableStateFlow<Set<Int>>(setOf(1, 2, 3, 4, 5, 6, 7))
    val selectedDays: StateFlow<Set<Int>> = _selectedDays.asStateFlow()

    private val _startDate = MutableStateFlow(System.currentTimeMillis())
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _trackSupply = MutableStateFlow(true)
    val trackSupply: StateFlow<Boolean> = _trackSupply.asStateFlow()

    private val _remainingSupply = MutableStateFlow("30")
    val remainingSupply: StateFlow<String> = _remainingSupply.asStateFlow()

    private val _lowSupplyThreshold = MutableStateFlow("5")
    val lowSupplyThreshold: StateFlow<String> = _lowSupplyThreshold.asStateFlow()

    private val _suggestions = MutableStateFlow<List<MedicineSuggestion>>(emptyList())
    val suggestions: StateFlow<List<MedicineSuggestion>> = _suggestions.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun loadMedication(medicationId: String) {
        editingMedicationId = medicationId
        viewModelScope.launch {
            val med = app.database.medicationDao().getMedicationById(medicationId) ?: return@launch
            _medName.value = med.name
            _doseDesc.value = med.doseDescription
            _scheduleType.value = med.scheduleType
            _times.value = if (med.times.isNotEmpty()) med.times else listOf("08:00")
            _selectedDays.value = med.daysOfWeek?.toSet() ?: setOf(1, 2, 3, 4, 5, 6, 7)
            _startDate.value = med.startDate
            _endDate.value = med.endDate
            _trackSupply.value = med.remainingSupply != null
            _remainingSupply.value = med.remainingSupply?.toString() ?: ""
            _lowSupplyThreshold.value = med.lowSupplyThreshold?.toString() ?: "5"
        }
    }

    fun onNameChanged(newName: String) {
        _medName.value = newName
        viewModelScope.launch {
            if (newName.length >= 2) {
                _suggestions.value = catalogRepo.searchMedicines(newName)
            } else {
                _suggestions.value = emptyList()
            }
        }
    }

    fun selectSuggestion(item: MedicineSuggestion) {
        _medName.value = item.name
        if (_doseDesc.value.isBlank()) {
            _doseDesc.value = item.defaultDose.ifBlank { "${item.form}, 1 unit" }
        }
        _suggestions.value = emptyList()
    }

    fun onDoseDescChanged(desc: String) {
        _doseDesc.value = desc
    }

    fun setScheduleType(type: ScheduleType) {
        _scheduleType.value = type
    }

    fun addTime(timeStr: String) {
        val current = _times.value.toMutableList()
        if (!current.contains(timeStr)) {
            current.add(timeStr)
            current.sort()
            _times.value = current
        }
    }

    fun removeTime(index: Int) {
        val current = _times.value.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _times.value = current
        }
    }

    fun toggleDay(day: Int) {
        val current = _selectedDays.value.toMutableSet()
        if (current.contains(day)) {
            if (current.size > 1) { // keep at least 1 day
                current.remove(day)
            }
        } else {
            current.add(day)
        }
        _selectedDays.value = current
    }

    fun setStartDate(millis: Long) {
        _startDate.value = millis
    }

    fun setEndDate(millis: Long?) {
        _endDate.value = millis
    }

    fun toggleTrackSupply(track: Boolean) {
        _trackSupply.value = track
    }

    fun onRemainingSupplyChanged(value: String) {
        _remainingSupply.value = value.filter { it.isDigit() }
    }

    fun onThresholdChanged(value: String) {
        _lowSupplyThreshold.value = value.filter { it.isDigit() }
    }

    fun save(profileId: String, profileName: String?, onSuccess: () -> Unit) {
        val name = _medName.value.trim()
        if (name.isBlank()) return

        _isSaving.value = true
        viewModelScope.launch {
            val supply = if (_trackSupply.value) _remainingSupply.value.toIntOrNull() else null
            val threshold = if (_trackSupply.value) _lowSupplyThreshold.value.toIntOrNull() ?: 5 else null

            val med = Medication(
                id = editingMedicationId ?: UUID.randomUUID().toString(),
                profileId = profileId,
                name = name,
                doseDescription = _doseDesc.value.ifBlank { "1 dose" },
                scheduleType = _scheduleType.value,
                times = if (_times.value.isNotEmpty()) _times.value else listOf("08:00"),
                daysOfWeek = if (_scheduleType.value == ScheduleType.SPECIFIC_DAYS) _selectedDays.value.toList() else null,
                startDate = _startDate.value,
                endDate = _endDate.value,
                remainingSupply = supply,
                lowSupplyThreshold = threshold,
                hasNotifiedLowSupply = false,
                isActive = true
            )

            if (editingMedicationId != null) {
                medRepo.updateMedication(med, profileName)
            } else {
                medRepo.addMedication(med, profileName)
            }

            _isSaving.value = false
            onSuccess()
        }
    }

    fun delete(medicationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            medRepo.deleteMedication(medicationId)
            onSuccess()
        }
    }
}
