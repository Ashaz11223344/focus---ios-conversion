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
import com.example.motivation.data.QuoteRepository
import java.util.Calendar

class QuoteWidgetProvider : AppWidgetProvider() {

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
        // Access the quotes directly from the QuoteRepository object
        val quotes = QuoteRepository.allQuotes
        if (quotes.isEmpty()) return

        // Get the quote for the current day
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val quote = quotes[dayOfYear % quotes.size]

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        // Note: You might need to update these layouts if they were designed for the old structure
        val layoutId = if (minWidth < 200) {
            R.layout.quote_widget
        } else if (minWidth < 300) {
            R.layout.quote_widget_medium
        } else {
            R.layout.quote_widget_large
        }

        val views = RemoteViews(context.packageName, layoutId)
        // Set the text directly from the quote string
        views.setTextViewText(R.id.quote_text, quote)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        views.setOnClickPendingIntent(R.id.quote_text, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
