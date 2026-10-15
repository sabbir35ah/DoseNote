package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.data.local.DoseNoteDatabase
import com.example.data.repository.CaregiverRepository
import com.example.data.repository.MedicationRepository
import com.example.data.repository.MedicineCatalogRepository
import com.example.data.repository.UserRepository
import com.example.notification.NotificationHelper
import com.example.worker.MissedDoseWorker
import com.example.worker.RollingScheduleWorker

class DoseNoteApplication : Application(), Configuration.Provider {

    val database by lazy { DoseNoteDatabase.getDatabase(this) }
    val userRepository by lazy { UserRepository(database) }
    val medicationRepository by lazy { MedicationRepository(this, database) }
    val caregiverRepository by lazy { CaregiverRepository(database, userRepository) }
    val medicineCatalogRepository by lazy { MedicineCatalogRepository(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Schedule periodic background tasks for 14-day rolling schedule top-up and missed dose flips
        try {
            RollingScheduleWorker.enqueuePeriodicWork(this)
            MissedDoseWorker.enqueuePeriodicWork(this)
        } catch (e: Exception) {
            Log.w("DoseNoteApplication", "WorkManager initialization deferred: ${e.message}")
        }
    }
}
