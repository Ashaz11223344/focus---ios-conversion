package com.example.motivation.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.motivation.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.focusGuardDao()

                    // 1. Reschedule DND alarms
                    val dndSchedules = dao.getAllDndSchedules().first()
                    val dndManager = DndScheduleManager(context)
                    dndManager.scheduleAll(dndSchedules)

                    // 2. Start AppBlockerService if there are enabled block rules
                    val appRules = dao.getEnabledAppBlockRules()
                    if (appRules.isNotEmpty()) {
                        AppBlockerService.start(context)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "Error in BootReceiver reschedule", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
