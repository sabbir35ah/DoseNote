package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.data.local.DoseNoteDatabase
import com.example.data.model.DoseStatus
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DoseAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DoseAlarmReceiver"

        const val ACTION_DOSE_ALARM = "com.aistudio.dosenote.ACTION_DOSE_ALARM"
        const val ACTION_TAKE_DOSE = "com.aistudio.dosenote.ACTION_TAKE_DOSE"
        const val ACTION_SKIP_DOSE = "com.aistudio.dosenote.ACTION_SKIP_DOSE"
        const val ACTION_SNOOZE_DOSE = "com.aistudio.dosenote.ACTION_SNOOZE_DOSE"

        const val EXTRA_DOSE_LOG_ID = "extra_dose_log_id"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSE_DESC = "extra_dose_desc"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val doseLogId = intent.getStringExtra(EXTRA_DOSE_LOG_ID) ?: return
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: ""
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medicine"
        val doseDesc = intent.getStringExtra(EXTRA_DOSE_DESC) ?: ""
        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, doseLogId.hashCode())

        Log.d(TAG, "onReceive action=$action doseLogId=$doseLogId")

        when (action) {
            ACTION_DOSE_ALARM -> {
                // Verify dose is still PENDING before alerting
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val db = DoseNoteDatabase.getDatabase(context)
                        val log = db.doseLogDao().getDoseLogById(doseLogId)
                        if (log != null && log.status == DoseStatus.PENDING) {
                            NotificationHelper.showDoseNotification(
                                context = context,
                                doseLogId = doseLogId,
                                medicationId = medicationId,
                                medName = medName,
                                doseDesc = doseDesc,
                                profileName = profileName
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_TAKE_DOSE -> {
                NotificationHelper.dismissNotification(context, notificationId)
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val db = DoseNoteDatabase.getDatabase(context)
                        db.doseLogDao().updateStatus(
                            id = doseLogId,
                            status = DoseStatus.TAKEN,
                            respondedAt = System.currentTimeMillis()
                        )
                        if (medicationId.isNotBlank()) {
                            db.medicationDao().decrementSupply(medicationId)
                            val med = db.medicationDao().getMedicationById(medicationId)
                            if (med != null &&
                                med.remainingSupply != null &&
                                med.lowSupplyThreshold != null &&
                                med.remainingSupply <= med.lowSupplyThreshold &&
                                !med.hasNotifiedLowSupply
                            ) {
                                db.medicationDao().markLowSupplyNotified(medicationId)
                                NotificationHelper.showLowSupplyNotification(
                                    context = context,
                                    medicationId = med.id,
                                    medName = med.name,
                                    remainingDoses = med.remainingSupply
                                )
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SKIP_DOSE -> {
                NotificationHelper.dismissNotification(context, notificationId)
                val pendingResult = goAsync()
                scope.launch {
                    try {
                        val db = DoseNoteDatabase.getDatabase(context)
                        db.doseLogDao().updateStatus(
                            id = doseLogId,
                            status = DoseStatus.SKIPPED,
                            respondedAt = System.currentTimeMillis()
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_DOSE -> {
                NotificationHelper.dismissNotification(context, notificationId)
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
        }
    }
}
