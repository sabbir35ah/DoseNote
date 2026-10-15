package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.DoseNoteDatabase
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.receiver.DoseAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleDoseAlarm(
        context: Context,
        doseLog: DoseLog,
        medication: Medication,
        profileName: String?
    ) {
        val now = System.currentTimeMillis()
        if (doseLog.scheduledAt < now) {
            // Already in the past
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // On Android 12+ check if exact alarms can be scheduled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarm: permission not granted")
                // Fallback to non-exact or standard schedule
            }
        }

        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = DoseAlarmReceiver.ACTION_DOSE_ALARM
            putExtra(DoseAlarmReceiver.EXTRA_DOSE_LOG_ID, doseLog.id)
            putExtra(DoseAlarmReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(DoseAlarmReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(DoseAlarmReceiver.EXTRA_DOSE_DESC, medication.doseDescription)
            putExtra(DoseAlarmReceiver.EXTRA_PROFILE_NAME, profileName)
        }

        val requestCode = doseLog.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    doseLog.scheduledAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    doseLog.scheduledAt,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled exact alarm for ${medication.name} at ${doseLog.scheduledAt}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm: ${e.message}")
        }
    }

    fun snoozeDoseAlarm(
        context: Context,
        doseLogId: String,
        medicationId: String,
        medName: String,
        doseDesc: String,
        profileName: String?,
        snoozeMinutes: Int = 15
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = DoseAlarmReceiver.ACTION_DOSE_ALARM
            putExtra(DoseAlarmReceiver.EXTRA_DOSE_LOG_ID, doseLogId)
            putExtra(DoseAlarmReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(DoseAlarmReceiver.EXTRA_MED_NAME, medName)
            putExtra(DoseAlarmReceiver.EXTRA_DOSE_DESC, doseDesc)
            putExtra(DoseAlarmReceiver.EXTRA_PROFILE_NAME, profileName)
        }

        val requestCode = (doseLogId + "_snooze").hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    snoozeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Snooze alarm failed: ${e.message}")
        }
    }

    fun cancelDoseAlarm(context: Context, doseLogId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = DoseAlarmReceiver.ACTION_DOSE_ALARM
        }
        val requestCode = doseLogId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    suspend fun rescheduleAllUpcomingAlarms(context: Context) = withContext(Dispatchers.IO) {
        val database = DoseNoteDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        val twoWeeksLater = now + (14 * 24 * 60 * 60 * 1000L)

        val pendingDoses = database.doseLogDao().getPendingDoseLogsInRange(now, twoWeeksLater)
        for (dose in pendingDoses) {
            val med = database.medicationDao().getMedicationById(dose.medicationId)
            if (med != null && med.isActive) {
                val profile = database.profileDao().getProfileById(med.profileId)
                scheduleDoseAlarm(
                    context = context,
                    doseLog = dose,
                    medication = med,
                    profileName = profile?.displayName
                )
            }
        }
    }
}
