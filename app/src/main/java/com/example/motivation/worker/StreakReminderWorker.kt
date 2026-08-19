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

        // Sunday Weekly Report Check
        val today = Calendar.getInstance()
        if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            try {
                val database = com.example.motivation.data.local.AppDatabase.getDatabase(applicationContext)
                val dao = database.motivationDao()
                
                val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfWeek = cal.timeInMillis
                val endOfWeek = startOfWeek + 7 * 86400000L - 1
                
                val moodLogs = dao.getMoodLogsBetween(startOfWeek, endOfWeek)
                if (moodLogs.isNotEmpty()) {
                    NotificationHelper(applicationContext).showNotification(
                        title = "Your Weekly Focus Report is ready ✦",
                        content = "Tap to view a summary of your week's emotional and journal journey.",
                        destination = "weekly_report/false"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("StreakReminderWorker", "Error in Sunday Weekly Report Check", e)
            }
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
