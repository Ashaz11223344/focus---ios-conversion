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
import com.example.motivation.data.QuoteRepository

class QuotesMediumWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            var quoteText = prefs.getString("current_quote_text", null)
            var quoteCategory = prefs.getString("current_quote_category", null)

            if (quoteText == null) {
                // Fallback / Initial load
                QuoteRepository.initialize(context)
                val defaultQuote = QuoteRepository.getRandomQuote()
                quoteText = defaultQuote.text
                quoteCategory = defaultQuote.category
                
                prefs.edit()
                    .putString("current_quote_text", quoteText)
                    .putString("current_quote_category", quoteCategory)
                    .apply()
            }

            val views = RemoteViews(context.packageName, R.layout.quotes_medium_widget)
            views.setTextViewText(R.id.widget_quote_text, quoteText)
            views.setTextViewText(R.id.widget_quote_category, quoteCategory)

            // Setup PendingIntent to open the main app activity on click
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
