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
        QuoteRepository.initialize(applicationContext)
        val quote = QuoteRepository.getRandomQuote()

        NotificationHelper(applicationContext).showNotification(
            "Daily Motivation",
            quote.text
        )

        return Result.success()
    }
}
