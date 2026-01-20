package com.example.motivation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.example.motivation.MainActivity
import com.example.motivation.R
import com.example.motivation.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StreakWidgetProvider : AppWidgetProvider() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        coroutineScope.launch {
            val settingsDataStore = SettingsDataStore(context)
            val streakCount = settingsDataStore.streakCount.first()
            val lastCompletion = settingsDataStore.lastCompletionDate.first()

            // Calculate days completed this week for the progress bar
            val daysSinceLastCompletion = if (lastCompletion == 0L) 7 else TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastCompletion)
            val weeklyProgress = (7 - daysSinceLastCompletion).coerceIn(0, 7)

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

            val layoutId = when {
                minWidth < 120 -> R.layout.streak_widget_small
                minWidth < 220 -> R.layout.streak_widget_medium
                else -> R.layout.streak_widget_large
            }

            val views = RemoteViews(context.packageName, layoutId)
            views.setTextViewText(R.id.streak_count_text, streakCount.toString())

            // Set progress only for medium and large widgets
            if (layoutId != R.layout.streak_widget_small) {
                views.setProgressBar(R.id.progress_bar, 7, weeklyProgress.toInt(), false)
            }
            if (layoutId == R.layout.streak_widget_large) {
                val lastDate = if(lastCompletion == 0L) "Never" else java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(lastCompletion))
                views.setTextViewText(R.id.last_completed_text, "Last: $lastDate")
            }

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
