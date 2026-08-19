package com.example.motivation.focusguard

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.NotificationManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.data.local.DndScheduleEntity
import java.util.Calendar

class DndScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll(schedules: List<DndScheduleEntity>) {
        schedules.forEach { schedule ->
            if (schedule.isEnabled) {
                scheduleDndStart(schedule)
                scheduleDndEnd(schedule)
            } else {
                cancelSchedule(schedule)
            }
        }
    }

    fun scheduleDndStart(schedule: DndScheduleEntity) {
        val triggerTime = nextOccurrenceMillis(schedule.startHour, schedule.startMinute, schedule.daysOfWeek)
        val intent = Intent(context, DndStartReceiver::class.java).apply {
            putExtra("schedule_id", schedule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id * 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    fun scheduleDndEnd(schedule: DndScheduleEntity) {
        val triggerTime = nextOccurrenceMillis(schedule.endHour, schedule.endMinute, schedule.daysOfWeek)
        val intent = Intent(context, DndEndReceiver::class.java).apply {
            putExtra("schedule_id", schedule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id * 2 + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    fun checkAndApplyCurrentDndState(schedules: List<DndScheduleEntity>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted) return

        val shouldBeActive = schedules.any { it.isEnabled && isCurrentlyInDndWindow(it) }
        val currentFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) nm.currentInterruptionFilter else -1

        if (shouldBeActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentFilter != NotificationManager.INTERRUPTION_FILTER_NONE) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                postDndStatusNotification(context, active = true)
                runBlocking {
                    SettingsDataStore(context).setDndActive(true)
                }
            }
        } else {
            val isAppActive = runBlocking {
                try {
                    SettingsDataStore(context).isDndActive.first()
                } catch(e: Exception) {
                    false
                }
            }
            if (isAppActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                postDndStatusNotification(context, active = false)
                runBlocking {
                    SettingsDataStore(context).setDndActive(false)
                }
            }
        }
    }

    private fun isCurrentlyInDndWindow(schedule: DndScheduleEntity): Boolean {
        val cal = Calendar.getInstance()
        val dayBit = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        if (schedule.daysOfWeek and (1 shl dayBit) == 0) return false

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = schedule.startHour * 60 + schedule.startMinute
        val endMinutes = schedule.endHour * 60 + schedule.endMinute

        return if (endMinutes > startMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Overnight DND
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    fun cancelSchedule(schedule: DndScheduleEntity) {
        val startIntent = Intent(context, DndStartReceiver::class.java)
        val startPi = PendingIntent.getBroadcast(
            context,
            schedule.id * 2,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPi != null) {
            alarmManager.cancel(startPi)
            startPi.cancel()
        }

        val endIntent = Intent(context, DndEndReceiver::class.java)
        val endPi = PendingIntent.getBroadcast(
            context,
            schedule.id * 2 + 1,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPi != null) {
            alarmManager.cancel(endPi)
            endPi.cancel()
        }
    }

    private fun nextOccurrenceMillis(hour: Int, minute: Int, daysOfWeekBitmask: Int): Long {
        val now = Calendar.getInstance()
        var bestTime = Long.MAX_VALUE
        
        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            val bitIndex = (dayOfWeek - Calendar.MONDAY + 7) % 7
            val isDaySet = (daysOfWeekBitmask and (1 shl bitIndex)) != 0
            
            if (isDaySet) {
                if (i == 0 && candidate.timeInMillis <= now.timeInMillis) {
                    continue
                }
                if (candidate.timeInMillis < bestTime) {
                    bestTime = candidate.timeInMillis
                }
            }
        }
        
        if (bestTime == Long.MAX_VALUE) {
            val fallback = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return fallback.timeInMillis
        }
        
        return bestTime
    }
}
