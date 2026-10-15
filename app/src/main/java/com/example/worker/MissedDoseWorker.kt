package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.DoseNoteDatabase
import java.util.concurrent.TimeUnit

class MissedDoseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Checking for missed doses")
        val db = DoseNoteDatabase.getDatabase(applicationContext)
        val cutoff = System.currentTimeMillis() - (30 * 60 * 1000L) // 30 minutes grace period
        val updatedCount = db.doseLogDao().markMissedDosesBefore(cutoff)
        Log.d(TAG, "Marked $updatedCount expired doses as MISSED")
        return Result.success()
    }

    companion object {
        private const val TAG = "MissedDoseWorker"
        const val WORK_NAME = "missed_dose_worker"

        fun enqueuePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MissedDoseWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
