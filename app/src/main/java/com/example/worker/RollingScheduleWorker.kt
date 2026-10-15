package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.DoseNoteDatabase
import com.example.data.util.ScheduleGenerator
import java.util.concurrent.TimeUnit

class RollingScheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting RollingScheduleWorker")
        val db = DoseNoteDatabase.getDatabase(applicationContext)
        val activeMeds = db.medicationDao().getAllActiveMedications()

        for (med in activeMeds) {
            val profile = db.profileDao().getProfileById(med.profileId)
            ScheduleGenerator.generateRollingScheduleForMedication(
                context = applicationContext,
                database = db,
                medication = med,
                profileName = profile?.displayName,
                daysAhead = 14
            )
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "RollingScheduleWorker"
        const val WORK_NAME = "rolling_schedule_worker"

        fun enqueuePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RollingScheduleWorker>(
                1, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
