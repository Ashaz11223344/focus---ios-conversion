package com.example.motivation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.example.motivation.MainActivity
import com.example.motivation.R
import com.example.motivation.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JournalMediumWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.motivationDao()

        CoroutineScope(Dispatchers.IO).launch {
            val latestEntry = dao.getLatestJournalEntryDirect()
            val entryText = latestEntry?.content ?: "Start writing your journal"
            val dateText = latestEntry?.dateDisplay ?: ""

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.journal_medium_widget)
                views.setTextViewText(R.id.widget_journal_content, entryText)
                
                if (latestEntry == null) {
                    views.setViewVisibility(R.id.widget_journal_date, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_journal_date, View.VISIBLE)
                    views.setTextViewText(R.id.widget_journal_date, dateText)
                }

                // Setup PendingIntent to open the app's Journal Screen on click
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("start_destination", "journal")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    3,
                    intent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
