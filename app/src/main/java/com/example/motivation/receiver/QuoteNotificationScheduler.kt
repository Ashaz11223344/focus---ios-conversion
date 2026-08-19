package com.example.motivation.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import com.example.motivation.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import kotlin.random.Random

object QuoteNotificationScheduler {
    private const val TAG = "QuoteNotifScheduler"
    private const val BASE_REQUEST_CODE = 10000
    const val ACTION_SHOW_QUOTE = "com.example.motivation.ACTION_SHOW_QUOTE"

    // Maximum slot index to defensively cancel — covers any historical request codes
    private const val MAX_DEFENSIVE_CANCEL_SLOTS = 20

    /**
     * Cancels ALL quote-related alarms and workers from every known source.
     * Uses wide-range defensive cancellation to catch any zombie PendingIntents.
     */
    fun cancelAll(context: Context) {
        Log.d(TAG, "[CANCEL] === cancelAll() START ===")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val workManager = try {
            WorkManager.getInstance(context)
        } catch (e: Exception) {
            Log.e(TAG, "[CANCEL] WorkManager unavailable: ${e.message}")
            null
        }

        // 1. Cancel AlarmManager PendingIntents for a WIDE range of request codes (1..20)
        //    This covers any historically used slot indices, not just the current 1..3
        for (i in 1..MAX_DEFENSIVE_CANCEL_SLOTS) {
            val intent = Intent(context, QuoteNotificationReceiver::class.java).apply {
                action = ACTION_SHOW_QUOTE
                putExtra("slot_index", i)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "[CANCEL] AlarmManager slot $i (requestCode=${BASE_REQUEST_CODE + i}) cancelled.")
        }

        // 2. Cancel WorkManager jobs by every known unique work name
        val workNamesToCancel = listOf(
            "MotivationNotificationWork",       // Legacy periodic worker
            "quote_notification_slot_1",         // Slot-based workers (if any were ever used)
            "quote_notification_slot_2",
            "quote_notification_slot_3"
        )
        for (name in workNamesToCancel) {
            workManager?.cancelUniqueWork(name)
            Log.d(TAG, "[CANCEL] WorkManager unique work '$name' cancelled.")
        }

        // 3. Cancel by tag — covers any workers tagged with these values
        val tagsToCancel = listOf(
            "motivation_notification",
            "quote_notification",
            "MotivationNotificationWorker"
        )
        for (tag in tagsToCancel) {
            workManager?.cancelAllWorkByTag(tag)
            Log.d(TAG, "[CANCEL] WorkManager tag '$tag' cancelled.")
        }

        Log.d(TAG, "[CANCEL] === cancelAll() COMPLETE ===")
    }

    /**
     * Aggressive purge designed to run on every app startup.
     * Nukes ALL possible zombie workers and stale alarms, then reschedules fresh.
     */
    fun aggressivePurgeOnStartup(context: Context) {
        Log.d(TAG, "[PURGE] === aggressivePurgeOnStartup() START ===")

        // Step 1: Cancel all known quote notification sources
        cancelAll(context)

        // Step 2: Extra paranoia — query and cancel any MotivationNotificationWork that might
        //         still be ENQUEUED or RUNNING in WorkManager's internal database
        try {
            val workManager = WorkManager.getInstance(context)

            // Check if legacy worker is still alive
            val workInfos = workManager.getWorkInfosForUniqueWork("MotivationNotificationWork").get()
            for (info in workInfos) {
                Log.w(TAG, "[PURGE] Found zombie MotivationNotificationWork: id=${info.id}, state=${info.state}")
                if (!info.state.isFinished) {
                    workManager.cancelWorkById(info.id)
                    Log.w(TAG, "[PURGE] Killed zombie worker by ID: ${info.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PURGE] Error checking for zombie workers: ${e.message}")
        }

        // Step 3: Reschedule fresh from current settings
        rescheduleAllQuoteNotifications(context)

        Log.d(TAG, "[PURGE] === aggressivePurgeOnStartup() COMPLETE ===")
    }

    fun rescheduleAllQuoteNotifications(context: Context) {
        // 1. Cancel everything first to prevent any phantom jobs
        cancelAll(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val settingsDataStore = SettingsDataStore(context)

        // 2. Read ALL settings in a single runBlocking block to minimize context switches
        //    (previously 9 separate runBlocking calls, each performing its own disk read)
        val enabled: Boolean
        val scheduleType: String
        val count: Int
        val qhEnabled: Boolean
        val qhStart: Int
        val qhEnd: Int
        val slotTimes: List<Int>

        runBlocking {
            enabled = settingsDataStore.quoteNotificationsEnabled.first()
            scheduleType = settingsDataStore.quoteScheduleType.first()
            count = settingsDataStore.notificationCountPerDay.first()
            qhEnabled = settingsDataStore.quietHoursEnabled.first()
            qhStart = settingsDataStore.quietHoursStart.first()
            qhEnd = settingsDataStore.quietHoursEnd.first()
            slotTimes = listOf(
                settingsDataStore.quoteTimeSlot1.first(),
                settingsDataStore.quoteTimeSlot2.first(),
                settingsDataStore.quoteTimeSlot3.first()
            )
        }

        if (!enabled) {
            Log.d(TAG, "Quote notifications are disabled. Reschedule aborted.")
            return
        }

        Log.d(TAG, "Fresh settings loaded. Enabled: $enabled, Mode: $scheduleType, Count: $count, SlotTimes: $slotTimes")


        // 3. Schedule ONLY the exact slots required — never more than count
        for (i in 1..count) {
            val savedTime = slotTimes[i - 1]
            val triggerMinutes = if (scheduleType == "Random" || savedTime == -1) {
                getRandomMinuteOutsideQuietHours(qhEnabled, qhStart, qhEnd)
            } else {
                savedTime
            }

            val targetHour = triggerMinutes / 60
            val targetMinute = triggerMinutes % 60

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Buffer to avoid immediate refiring if scheduling very close to the alarm time
            if (calendar.timeInMillis <= System.currentTimeMillis() + 5000) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(context, QuoteNotificationReceiver::class.java).apply {
                action = ACTION_SHOW_QUOTE
                putExtra("slot_index", i)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel specifically before scheduling to satisfy "REPLACE" or "cancel-first" rule
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(TAG, "Cannot schedule exact alarm: exact alarm permission not granted.")
                    try {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Fallback inexact scheduled slot $i at: ${calendar.time}")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Failed to schedule inexact alarm: ${e.message}")
                    }
                    continue
                }
            }

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled slot $i at: ${calendar.time} (random=${savedTime == -1})")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException scheduling exact alarm: ${e.message}. Falling back to inexact.")
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Fallback scheduled slot $i at: ${calendar.time}")
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to schedule fallback: ${ex.message}")
                }
            }
        }

        Log.d(TAG, "Scheduling complete. Exactly $count slot(s) scheduled.")
    }

    private fun getRandomMinuteOutsideQuietHours(qhEnabled: Boolean, qhStart: Int, qhEnd: Int): Int {
        if (!qhEnabled) {
            return Random.nextInt(24 * 60)
        }
        val validMinutes = mutableListOf<Int>()
        for (m in 0 until 24 * 60) {
            val inQH = if (qhStart < qhEnd) {
                m in qhStart..qhEnd
            } else {
                m >= qhStart || m <= qhEnd
            }
            if (!inQH) {
                validMinutes.add(m)
            }
        }
        return if (validMinutes.isNotEmpty()) {
            validMinutes.random()
        } else {
            Random.nextInt(24 * 60)
        }
    }
}
