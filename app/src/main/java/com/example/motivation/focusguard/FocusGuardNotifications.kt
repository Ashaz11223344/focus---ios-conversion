package com.example.motivation.focusguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.motivation.R

private const val CHANNEL_ID = "focus_guard_status"

fun ensureFocusGuardStatusChannel(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Focus Guard Status",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Alerts when Focus Guard activates or deactivates"
        setShowBadge(false)
    }
    nm.createNotificationChannel(channel)
}

fun postDndStatusNotification(context: Context, active: Boolean) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    ensureFocusGuardStatusChannel(context)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_splash_logo)
        .setContentTitle(if (active) "Focus Guard: DND Active" else "Focus Guard: DND Ended")
        .setContentText(
            if (active) "All notifications are silenced. Stay focused."
            else "Notification silencing has ended. Welcome back."
        )
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setColor(0xFFFC6E20.toInt())
        .build()

    nm.notify(9002, notification)
}

fun postAppBlockedNotification(context: Context, appName: String, endHour: Int, endMinute: Int) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    ensureFocusGuardStatusChannel(context)

    val untilTime = String.format("%02d:%02d", endHour, endMinute)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_splash_logo)
        .setContentTitle("$appName is blocked")
        .setContentText("Blocked until $untilTime. Keep going.")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setColor(0xFFFC6E20.toInt())
        .build()

    nm.notify(9003, notification)
}
