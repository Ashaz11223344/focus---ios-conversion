package com.example.motivation.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.motivation.MainActivity
import com.example.motivation.R
import com.example.motivation.data.local.Achievement

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val GENERAL_CHANNEL_ID = "general_channel"
        const val ACHIEVEMENT_CHANNEL_ID = "achievement_channel"
        const val MOOD_REMINDER_CHANNEL_ID = "mood_reminder_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.notification)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val generalChannel = NotificationChannel(GENERAL_CHANNEL_ID, "General Notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "General app notifications"
                setSound(soundUri, audioAttributes)
            }

            val achievementChannel = NotificationChannel(ACHIEVEMENT_CHANNEL_ID, "Achievement Unlocks", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications for when you unlock an achievement"
                setSound(soundUri, audioAttributes)
            }

            val moodReminderChannel = NotificationChannel(MOOD_REMINDER_CHANNEL_ID, "Mood Reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily reminder to log your mood"
                setSound(soundUri, audioAttributes)
            }
            
            notificationManager.createNotificationChannel(generalChannel)
            notificationManager.createNotificationChannel(achievementChannel)
            notificationManager.createNotificationChannel(moodReminderChannel)
        }
    }

    fun showNotification(
        title: String,
        content: String,
        destination: String? = null,
        quoteText: String? = null,
        quoteCategory: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (destination != null) {
                putExtra("start_destination", destination)
            }
            if (quoteText != null) {
                putExtra("quote_text", quoteText)
            }
            if (quoteCategory != null) {
                putExtra("quote_category", quoteCategory)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            content.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_quote_mark)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(content.hashCode(), notification)
    }

    fun showAchievementNotification(achievement: Achievement) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("start_destination", "achievements")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            achievement.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ACHIEVEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_achievements)
            .setContentTitle("Achievement Unlocked!")
            .setContentText("You've unlocked: ${achievement.title}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("You've shown up for yourself and unlocked the ${achievement.title} achievement. Keep up the great work!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(achievement.hashCode(), notification)
    }
}
