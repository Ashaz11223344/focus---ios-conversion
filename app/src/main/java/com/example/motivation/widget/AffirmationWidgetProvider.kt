package com.example.motivation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.motivation.MainActivity
import com.example.motivation.R
import com.example.motivation.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AffirmationWidgetProvider : AppWidgetProvider() {

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

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        coroutineScope.launch {
            val settingsDataStore = SettingsDataStore(context)
            val userName = settingsDataStore.userName.first()

            if (userName.isNotBlank()) {
                val affirmation = getTodaysAffirmation(userName)
                val views = RemoteViews(context.packageName, R.layout.affirmation_widget)
                views.setTextViewText(R.id.affirmation_text, affirmation)

                // Make the widget clickable to open the affirmations screen
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("start_destination", "name_affirmations")
                }
                val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun getTodaysAffirmation(name: String): String {
        val affirmations = listOf(
            "$name is disciplined.",
            "$name finishes what he starts.",
            "$name doesn’t need motivation, he has habits."
        )
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return affirmations[dayOfYear % affirmations.size]
    }
}
