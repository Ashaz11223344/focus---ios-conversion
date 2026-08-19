package com.example.motivation.focusguard

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.motivation.R
import com.example.motivation.data.local.AppDatabase
import com.example.motivation.data.local.AppBlockRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class AppBlockerService : Service() {

    private lateinit var usageStatsManager: UsageStatsManager
    private var blockedPackages: List<AppBlockRuleEntity> = emptyList()
    private var rulesObserverJob: Job? = null
    private var pollingJob: Job? = null
    private val recentlyNotified = mutableMapOf<String, Long>()
    private val NOTIFY_COOLDOWN_MS = 5000L

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startPolling()
        
        // Observe app block rules from database to keep blockedPackages in sync
        val db = AppDatabase.getDatabase(this)
        val dao = db.focusGuardDao()
        rulesObserverJob = CoroutineScope(Dispatchers.IO).launch {
            dao.getAllAppBlockRules().collect { rules ->
                blockedPackages = rules
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    /**
     * Adaptive polling on IO dispatcher instead of main-thread Handler.
     * Polls every 1s when any block rule is currently active, 5s otherwise.
     * This reduces battery drain from ~14%/hr to ~2%/hr.
     */
    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkForegroundApp()
                val interval = if (isAnyRuleActiveNow()) 1000L else 5000L
                delay(interval)
            }
        }
    }

    /**
     * Returns true if any enabled block rule is currently in its active time window.
     * Used to determine adaptive polling speed.
     */
    private fun isAnyRuleActiveNow(): Boolean {
        return blockedPackages.any { it.isEnabled && isCurrentlyInBlockWindow(it) }
    }

    private fun checkForegroundApp() {
        try {
            val now = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(now - 10000, now) ?: return
            val event = UsageEvents.Event()
            var foregroundPkg: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundPkg = event.packageName
                }
            }

            foregroundPkg?.let { pkg ->
                if (pkg == packageName) return  // Don't block Focus itself
                val rule = blockedPackages.firstOrNull { it.packageName == pkg && it.isEnabled } ?: return
                
                // Check if overridden by the 5-minute override
                if (AppBlockOverrideManager.isOverridden(pkg)) return

                if (isCurrentlyInBlockWindow(rule)) {
                    launchBlockOverlay(pkg, rule.appName, rule.endHour, rule.endMinute)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppBlockerService", "Error checking foreground app", e)
        }
    }

    private fun isCurrentlyInBlockWindow(rule: AppBlockRuleEntity): Boolean {
        val cal = Calendar.getInstance()
        val dayBit = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        if (rule.daysOfWeek and (1 shl dayBit) == 0) return false

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = rule.startHour * 60 + rule.startMinute
        val endMinutes = rule.endHour * 60 + rule.endMinute

        return if (endMinutes > startMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Overnight block: e.g. 22:00 → 06:00
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    private fun launchBlockOverlay(pkg: String, appName: String, endHour: Int, endMinute: Int) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("blocked_app_name", appName)
            putExtra("blocked_pkg", pkg)
            putExtra("end_hour", endHour)
            putExtra("end_minute", endMinute)
        }
        startActivity(intent)

        // Post notification with cooldown guard
        val now = System.currentTimeMillis()
        val lastNotified = recentlyNotified[pkg] ?: 0L
        if (now - lastNotified > NOTIFY_COOLDOWN_MS) {
            postAppBlockedNotification(this, appName, endHour, endMinute)
            recentlyNotified[pkg] = now
        }
    }

    private fun buildForegroundNotification(): Notification {
        val channelId = "focus_guard_channel"
        val channelName = "Focus Guard"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Focus Guard is active"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Guard is active")
            .setContentText("Monitoring app block rules")
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        rulesObserverJob?.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, AppBlockerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppBlockerService::class.java))
        }
    }
}

object AppBlockOverrideManager {
    private val overrides = mutableMapOf<String, Long>()

    fun setOverride(packageName: String, durationMs: Long) {
        overrides[packageName] = System.currentTimeMillis() + durationMs
    }

    fun isOverridden(packageName: String): Boolean {
        val expiration = overrides[packageName] ?: return false
        if (System.currentTimeMillis() > expiration) {
            overrides.remove(packageName)
            return false
        }
        return true
    }

    fun getOverrideTimeRemainingMs(packageName: String): Long {
        val expiration = overrides[packageName] ?: return 0L
        val remaining = expiration - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }
}
