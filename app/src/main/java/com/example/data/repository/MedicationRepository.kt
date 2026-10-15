package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.DoseNoteDatabase
import com.example.data.model.DoseItem
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.data.util.ScheduleGenerator
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class AdherenceSummary(
    val totalScheduled: Int,
    val totalTaken: Int,
    val adherencePercentage: Int,
    val perMedicationBreakdown: List<MedicationAdherence>
)

data class MedicationAdherence(
    val medicationName: String,
    val scheduled: Int,
    val taken: Int,
    val percentage: Int
)

class MedicationRepository(
    private val context: Context,
    private val database: DoseNoteDatabase
) {

    fun getMedicationsForProfile(profileId: String): Flow<List<Medication>> {
        return database.medicationDao().getMedicationsByProfileFlow(profileId)
    }

    fun getLowSupplyMedications(profileId: String): Flow<List<Medication>> {
        return database.medicationDao().getLowSupplyMedicationsFlow(profileId)
    }

    fun getDoseItemsForProfileAndRange(
        profileId: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<DoseItem>> {
        val logsFlow = database.doseLogDao().getLogsForProfileAndRangeFlow(profileId, startTime, endTime)
        val medsFlow = database.medicationDao().getMedicationsByProfileFlow(profileId)

        return combine(logsFlow, medsFlow) { logs, meds ->
            val medMap = meds.associateBy { it.id }
            logs.mapNotNull { log ->
                val med = medMap[log.medicationId] ?: return@mapNotNull null
                DoseItem(doseLog = log, medication = med)
            }
        }
    }

    suspend fun addMedication(medication: Medication, profileName: String?) {
        database.medicationDao().insertOrUpdate(medication)
        ScheduleGenerator.generateRollingScheduleForMedication(
            context = context,
            database = database,
            medication = medication,
            profileName = profileName,
            daysAhead = 14
        )
    }

    suspend fun updateMedication(medication: Medication, profileName: String?) {
        database.medicationDao().update(medication)
        // Remove old future doses and regenerate
        val now = System.currentTimeMillis()
        database.doseLogDao().deleteFutureLogsForMedication(medication.id, now)
        ScheduleGenerator.generateRollingScheduleForMedication(
            context = context,
            database = database,
            medication = medication,
            profileName = profileName,
            daysAhead = 14
        )
    }

    suspend fun deleteMedication(medicationId: String) {
        val med = database.medicationDao().getMedicationById(medicationId) ?: return
        database.medicationDao().delete(med)
        database.doseLogDao().deleteAllLogsForMedication(medicationId)
        AlarmScheduler.cancelDoseAlarm(context, medicationId)
    }

    suspend fun markDoseTaken(doseLogId: String, medicationId: String) {
        val now = System.currentTimeMillis()
        database.doseLogDao().updateStatus(
            id = doseLogId,
            status = DoseStatus.TAKEN,
            respondedAt = now
        )

        // Decrement supply if tracked
        database.medicationDao().decrementSupply(medicationId)
        val med = database.medicationDao().getMedicationById(medicationId)
        if (med != null &&
            med.remainingSupply != null &&
            med.lowSupplyThreshold != null &&
            med.remainingSupply <= med.lowSupplyThreshold &&
            !med.hasNotifiedLowSupply
        ) {
            database.medicationDao().markLowSupplyNotified(medicationId)
            NotificationHelper.showLowSupplyNotification(
                context = context,
                medicationId = med.id,
                medName = med.name,
                remainingDoses = med.remainingSupply
            )
        }
    }

    suspend fun markDoseSkipped(doseLogId: String) {
        database.doseLogDao().updateStatus(
            id = doseLogId,
            status = DoseStatus.SKIPPED,
            respondedAt = System.currentTimeMillis()
        )
    }

    fun snoozeDose(
        doseLogId: String,
        medicationId: String,
        medName: String,
        doseDesc: String,
        profileName: String?
    ) {
        AlarmScheduler.snoozeDoseAlarm(
            context = context,
            doseLogId = doseLogId,
            medicationId = medicationId,
            medName = medName,
            doseDesc = doseDesc,
            profileName = profileName,
            snoozeMinutes = 15
        )
    }

    suspend fun calculateAdherence(profileId: String, days: Int = 7): AdherenceSummary {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -days)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val logs = database.doseLogDao().getLogsForProfileAndRange(profileId, startTime, endTime)
        val meds = database.medicationDao().getMedicationsByProfile(profileId).associateBy { it.id }

        val totalScheduled = logs.size
        val totalTaken = logs.count { it.status == DoseStatus.TAKEN }
        val overallPercentage = if (totalScheduled > 0) ((totalTaken * 100) / totalScheduled) else 100

        val perMedList = logs.groupBy { it.medicationId }.mapNotNull { (medId, medLogs) ->
            val medName = meds[medId]?.name ?: "Medicine"
            val scheduled = medLogs.size
            val taken = medLogs.count { it.status == DoseStatus.TAKEN }
            val pct = if (scheduled > 0) ((taken * 100) / scheduled) else 0
            MedicationAdherence(medicationName = medName, scheduled = scheduled, taken = taken, percentage = pct)
        }

        return AdherenceSummary(
            totalScheduled = totalScheduled,
            totalTaken = totalTaken,
            adherencePercentage = overallPercentage,
            perMedicationBreakdown = perMedList
        )
    }

    suspend fun checkMissedDoses() {
        val cutoff = System.currentTimeMillis() - (30 * 60 * 1000L)
        database.doseLogDao().markMissedDosesBefore(cutoff)
    }
}
