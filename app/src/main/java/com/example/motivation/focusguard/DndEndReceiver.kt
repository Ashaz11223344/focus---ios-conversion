package com.example.motivation.focusguard

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.motivation.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DndEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getIntExtra("schedule_id", -1)
        if (scheduleId == -1) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.focusGuardDao()
                val schedule = dao.getDndScheduleById(scheduleId)
                if (schedule != null && schedule.isEnabled) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (nm.isNotificationPolicyAccessGranted) {
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        postDndStatusNotification(context, active = false)
                        kotlinx.coroutines.runBlocking {
                            com.example.motivation.data.SettingsDataStore(context).setDndActive(false)
                        }
                    }
                    // Reschedule next occurrence
                    val manager = DndScheduleManager(context)
                    manager.scheduleDndEnd(schedule)
                }
            } catch (e: Exception) {
                android.util.Log.e("DndEndReceiver", "Error handling DND end alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
