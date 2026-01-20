package com.example.motivation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.helper.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

class StreakReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsDataStore = SettingsDataStore(applicationContext)
        val lastCompletion = settingsDataStore.lastCompletionDate.first()

        if (!isToday(lastCompletion)) {
            NotificationHelper(applicationContext).showNotification(
                title = "Don't Lose Your Streak!",
                content = "Your daily intent is waiting for you.",
                destination = "streak"
            )
        }

        return Result.success()
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val today = Calendar.getInstance()
        val other = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }
}
