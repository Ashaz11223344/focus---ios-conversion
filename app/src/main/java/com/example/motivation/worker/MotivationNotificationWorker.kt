package com.example.motivation.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * LEGACY WORKER — This worker is no longer used for scheduling quote notifications.
 * Quote notifications are now managed exclusively via AlarmManager + QuoteNotificationReceiver.
 *
 * This class is kept ONLY as a self-destruct guard: if a zombie instance of this worker
 * fires from WorkManager's internal database (e.g. restored from cloud backup), it will:
 * 1. Log a warning
 * 2. Cancel itself permanently
 * 3. Return success WITHOUT showing any notification
 */
class MotivationNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.w(
            "MotivationNotifWorker",
            "[ZOMBIE] MotivationNotificationWorker fired unexpectedly! " +
            "This is a legacy worker that should have been cancelled. " +
            "ID=${id}, runAttemptCount=$runAttemptCount. " +
            "Suppressing notification and self-destructing."
        )

        // Attempt to cancel this worker permanently so it never fires again
        try {
            val workManager = WorkManager.getInstance(applicationContext)
            workManager.cancelUniqueWork("MotivationNotificationWork")
            workManager.cancelWorkById(id)
            Log.w("MotivationNotifWorker", "[ZOMBIE] Self-destruct complete. Worker cancelled by ID and name.")
        } catch (e: Exception) {
            Log.e("MotivationNotifWorker", "[ZOMBIE] Failed to self-destruct: ${e.message}")
        }

        // Return success WITHOUT showing any notification — kills the phantom
        return Result.success()
    }
}
