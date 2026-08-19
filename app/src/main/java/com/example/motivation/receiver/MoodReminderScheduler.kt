package com.example.motivation.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.motivation.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object MoodReminderScheduler {
    private const val ALARM_REQUEST_CODE = 45678
    const val ACTION_SHOW_MOOD_REMINDER = "com.example.motivation.ACTION_SHOW_MOOD_REMINDER"

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val settingsDataStore = SettingsDataStore(context)

        // Read preferences synchronously using runBlocking
        val enabled = runBlocking { settingsDataStore.moodReminderEnabled.first() }
        val reminderMinutes = runBlocking { settingsDataStore.moodReminderTime.first() }
        val qhEnabled = runBlocking { settingsDataStore.quietHoursEnabled.first() }
        val qhStart = runBlocking { settingsDataStore.quietHoursStart.first() }
        val qhEnd = runBlocking { settingsDataStore.quietHoursEnd.first() }

        val intent = Intent(context, MoodReminderReceiver::class.java).apply {
            action = ACTION_SHOW_MOOD_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            Log.d("MoodReminderScheduler", "Mood reminder disabled. Cancelled alarm.")
            return
        }

        val targetHour = reminderMinutes / 60
        val targetMinute = reminderMinutes % 60

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Add 5 seconds buffer to avoid immediate refiring if scheduling very close to the alarm time
        if (calendar.timeInMillis <= System.currentTimeMillis() + 5000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // --- Quiet Hours Check and Adjustment ---
        val triggerMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val isInQuietHours = if (qhEnabled) {
            if (qhStart < qhEnd) {
                triggerMinutes in qhStart..qhEnd
            } else {
                triggerMinutes >= qhStart || triggerMinutes <= qhEnd
            }
        } else {
            false
        }

        if (isInQuietHours) {
            val qhEndHour = qhEnd / 60
            val qhEndMinute = qhEnd % 60

            val adjustedCalendar = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, qhEndHour)
                set(Calendar.MINUTE, qhEndMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (adjustedCalendar.before(calendar)) {
                adjustedCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (adjustedCalendar.timeInMillis <= System.currentTimeMillis() + 5000) {
                adjustedCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            calendar.timeInMillis = adjustedCalendar.timeInMillis
            Log.d("MoodReminderScheduler", "Alarm falls in Quiet Hours. Adjusted trigger to end of Quiet Hours: ${calendar.time}")
        }

        // Check if we have SCHEDULE_EXACT_ALARM permission on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("MoodReminderScheduler", "Cannot schedule exact alarm: exact alarm permission not granted.")
                // Fall back to standard setAndAllowWhileIdle if exact isn't allowed, to avoid crashing.
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    Log.e("MoodReminderScheduler", "Failed to schedule inexact alarm: ${e.message}")
                }
                return
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("MoodReminderScheduler", "Scheduled daily exact alarm at: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("MoodReminderScheduler", "SecurityException scheduling exact alarm: ${e.message}. Falling back to inexact alarm.")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e("MoodReminderScheduler", "Failed to schedule fallback inexact alarm: ${ex.message}")
            }
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MoodReminderReceiver::class.java).apply {
            action = ACTION_SHOW_MOOD_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("MoodReminderScheduler", "Cancelled mood reminder alarm.")
    }
}
