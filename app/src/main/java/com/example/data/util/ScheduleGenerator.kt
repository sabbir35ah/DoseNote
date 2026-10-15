package com.example.data.util

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.DoseNoteDatabase
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.data.model.ScheduleType
import java.util.Calendar
import java.util.UUID

object ScheduleGenerator {

    /**
     * Generates rolling 14 days of DoseLog records for a medication,
     * avoiding duplicate inserts, and schedules upcoming alarms.
     */
    suspend fun generateRollingScheduleForMedication(
        context: Context,
        database: DoseNoteDatabase,
        medication: Medication,
        profileName: String?,
        daysAhead: Int = 14
    ) {
        if (!medication.isActive) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If medication starts in the future, start generation from that day
        val startCal = Calendar.getInstance().apply {
            timeInMillis = medication.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (startCal.after(calendar)) {
            calendar.timeInMillis = startCal.timeInMillis
        }

        val logsToInsert = mutableListOf<DoseLog>()

        for (dayOffset in 0 until daysAhead) {
            val currentDayMillis = calendar.timeInMillis

            // If past medication end date, stop
            if (medication.endDate != null && currentDayMillis > medication.endDate) {
                break
            }

            // Check if day matches schedule
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat
            val dayMatches = when (medication.scheduleType) {
                ScheduleType.FIXED_TIMES -> true
                ScheduleType.SPECIFIC_DAYS -> medication.daysOfWeek?.contains(dayOfWeek) == true
            }

            if (dayMatches) {
                for (timeStr in medication.times) {
                    val parts = timeStr.split(":")
                    if (parts.size >= 2) {
                        val hour = parts[0].toIntOrNull() ?: 8
                        val minute = parts[1].toIntOrNull() ?: 0

                        val doseCal = Calendar.getInstance().apply {
                            timeInMillis = calendar.timeInMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val doseMillis = doseCal.timeInMillis
                        if (doseMillis >= medication.startDate &&
                            (medication.endDate == null || doseMillis <= medication.endDate)
                        ) {
                            // Check if already in DB
                            val existing = database.doseLogDao().getLogByMedAndSchedule(medication.id, doseMillis)
                            if (existing == null) {
                                val newLog = DoseLog(
                                    id = UUID.randomUUID().toString(),
                                    medicationId = medication.id,
                                    profileId = medication.profileId,
                                    scheduledAt = doseMillis,
                                    status = DoseStatus.PENDING
                                )
                                logsToInsert.add(newLog)
                            }
                        }
                    }
                }
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (logsToInsert.isNotEmpty()) {
            database.doseLogDao().insertAll(logsToInsert)
            for (log in logsToInsert) {
                if (log.scheduledAt > now) {
                    AlarmScheduler.scheduleDoseAlarm(context, log, medication, profileName)
                }
            }
        }
    }
}
