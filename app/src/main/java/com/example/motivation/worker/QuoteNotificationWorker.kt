package com.example.motivation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.motivation.data.QuoteRepository
import com.example.motivation.helper.NotificationHelper
import java.util.Calendar

class QuoteNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Access the list of strings directly from the static repository object
        val quotes = QuoteRepository.allQuotes
        if (quotes.isEmpty()) {
            return Result.failure() // No quotes to show
        }

        // Get the quote for the current day
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val quote = quotes[dayOfYear % quotes.size]

        // Show the notification using the quote string directly
        NotificationHelper(applicationContext).showNotification(
            "Daily Motivation",
            quote
        )

        return Result.success()
    }
}
