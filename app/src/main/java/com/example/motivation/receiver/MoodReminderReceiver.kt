package com.example.motivation.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.motivation.MainActivity
import com.example.motivation.R
import com.example.motivation.helper.NotificationHelper
import kotlin.random.Random

class MoodReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("MoodReminderReceiver", "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MoodReminderScheduler.schedule(context)
        } else if (action == MoodReminderScheduler.ACTION_SHOW_MOOD_REMINDER) {
            showNotification(context)
            MoodReminderScheduler.schedule(context)
        }
    }

    private fun showNotification(context: Context) {
        val messages = listOf(
            "Take a moment to check in with yourself.",
            "Your mood log is waiting — just a tap away.",
            "A small reflection goes a long way. Log your mood.",
            "Track how you feel — your future self will thank you."
        )
        val randomMessage = messages[Random.nextInt(messages.size)]

        val title = "How are you feeling today? 🌤"

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("start_destination", "mood")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Initialize NotificationHelper to guarantee that the mood reminder notification channel is created
        NotificationHelper(context)

        val notification = NotificationCompat.Builder(context, NotificationHelper.MOOD_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_quote_mark)
            .setContentTitle(title)
            .setContentText(randomMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(randomMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(title.hashCode(), notification)
        Log.d("MoodReminderReceiver", "Mood reminder notification shown.")
    }
}
