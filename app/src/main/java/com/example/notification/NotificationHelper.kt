package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.DoseAlarmReceiver

object NotificationHelper {
    const val CHANNEL_DOSE_REMINDERS = "channel_dose_reminders"
    const val CHANNEL_SUPPLY_ALERTS = "channel_supply_alerts"

    const val EXTRA_DOSE_LOG_ID = "extra_dose_log_id"
    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_MED_NAME = "extra_med_name"
    const val EXTRA_DOSE_DESC = "extra_dose_desc"
    const val EXTRA_PROFILE_NAME = "extra_profile_name"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val doseChannel = NotificationChannel(
                CHANNEL_DOSE_REMINDERS,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableVibration(true)
                enableLights(true)
            }

            val supplyChannel = NotificationChannel(
                CHANNEL_SUPPLY_ALERTS,
                context.getString(R.string.notif_channel_supply_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_supply_desc)
            }

            notificationManager?.createNotificationChannel(doseChannel)
            notificationManager?.createNotificationChannel(supplyChannel)
        }
    }

    fun showDoseNotification(
        context: Context,
        doseLogId: String,
        medicationId: String,
        medName: String,
        doseDesc: String,
        profileName: String?
    ) {
        val notificationId = doseLogId.hashCode()

        // Content intent: open MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOSE_LOG_ID, doseLogId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Taken
        val takeIntent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = DoseAlarmReceiver.ACTION_TAKE_DOSE
            putExtra(EXTRA_DOSE_LOG_ID, doseLogId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Skip
        val skipIntent = Intent(context, DoseAlarmReceiver::class.java).apply {
            action = DoseAlarmReceiver.ACTION_SKIP_DOSE
            putExtra(EXTRA_DOSE_LOG_ID, doseLogId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = context.getString(R.string.notif_dose_reminder_title, medName)
        val bodyText = if (!profileName.isNullOrBlank()) {
            "For $profileName • $doseDesc"
        } else {
            doseDesc
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DOSE_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$bodyText\nTap to open DoseNote or respond directly:"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.notif_action_taken),
                takePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notif_action_skip),
                skipPendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission might be denied on Android 13+
        }
    }

    fun showLowSupplyNotification(
        context: Context,
        medicationId: String,
        medName: String,
        remainingDoses: Int
    ) {
        val notificationId = ("supply_$medicationId").hashCode()
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notif_supply_low_title, medName)
        val body = context.getString(R.string.notif_supply_low_body, remainingDoses, medName)

        val builder = NotificationCompat.Builder(context, CHANNEL_SUPPLY_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
