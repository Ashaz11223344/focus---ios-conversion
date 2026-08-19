package com.example.motivation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.motivation.data.QuoteRepository
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.helper.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class QuoteNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "QuoteNotifReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "[RECEIVE] Broadcast received. action=$action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "[RECEIVE] Boot/Package replaced — rescheduling via aggressivePurge.")
            QuoteNotificationScheduler.aggressivePurgeOnStartup(context)
        } else if (action == QuoteNotificationScheduler.ACTION_SHOW_QUOTE) {
            val slotIndex = intent.getIntExtra("slot_index", -1)
            Log.d(TAG, "[RECEIVE] ACTION_SHOW_QUOTE fired. slotIndex=$slotIndex")

            val settingsDataStore = SettingsDataStore(context)

            // Read preferences synchronously
            val enabled = runBlocking { settingsDataStore.quoteNotificationsEnabled.first() }
            val count = runBlocking { settingsDataStore.notificationCountPerDay.first() }

            Log.d(TAG, "[RECEIVE] Settings check: enabled=$enabled, count=$count, slotIndex=$slotIndex, valid=${slotIndex in 1..count}")

            // Guard: ONLY show notification if slot is valid AND within configured count
            if (!enabled) {
                Log.d(TAG, "[RECEIVE] Quote notifications disabled. Suppressing.")
            } else if (slotIndex !in 1..count) {
                Log.w(TAG, "[RECEIVE] PHANTOM DETECTED! slotIndex=$slotIndex is outside valid range 1..$count. Suppressing notification.")
            } else {
                // Quiet Hours check
                val qhEnabled = runBlocking { settingsDataStore.quietHoursEnabled.first() }
                val qhStart = runBlocking { settingsDataStore.quietHoursStart.first() }
                val qhEnd = runBlocking { settingsDataStore.quietHoursEnd.first() }

                val now = Calendar.getInstance()
                val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

                val isInQuietHours = if (qhEnabled) {
                    if (qhStart < qhEnd) {
                        currentMinutes in qhStart..qhEnd
                    } else {
                        currentMinutes >= qhStart || currentMinutes <= qhEnd
                    }
                } else {
                    false
                }

                if (isInQuietHours) {
                    Log.d(TAG, "[RECEIVE] Quiet Hours active (current=$currentMinutes, start=$qhStart, end=$qhEnd). Suppressing.")
                } else {
                    // Show notification
                    runBlocking {
                        val selectedRaw = settingsDataStore.selectedGenreCategories.first()
                        val selectedCategories = if (selectedRaw.isBlank()) emptyList()
                                                 else selectedRaw.split(",")
                        val pointer = settingsDataStore.genreRotationPointer.first()
                        val slotsToday = count

                        val todayCategories = CategoryRotationEngine.getTodayCategories(
                            selectedCategories, pointer, slotsToday
                        )
                        val categoryForThisSlot = todayCategories.getOrNull(slotIndex - 1)

                        QuoteRepository.initialize(context)
                        val quote = if (categoryForThisSlot != null) {
                            QuoteRepository.getRandomQuoteByCategory(categoryForThisSlot)
                        } else {
                            QuoteRepository.getRandomQuote()
                        }

                        val title = "Focus"
                        val content = quote.text
                        val destination = "quotes"

                        NotificationHelper(context).showNotification(
                            title = title,
                            content = content,
                            destination = destination,
                            quoteText = quote.text,
                            quoteCategory = quote.category
                        )
                        Log.d(TAG, "[RECEIVE] ✅ Quote notification SHOWN for slot $slotIndex. content=\"${content.take(50)}...\"")
                    }
                }
            }

            // After all slots for the day have been scheduled (i.e. slotIndex == count), advance pointer
            val selectedRaw = runBlocking { settingsDataStore.selectedGenreCategories.first() }
            val selectedCategories = if (selectedRaw.isBlank()) emptyList() else selectedRaw.split(",")
            if (slotIndex == count && selectedCategories.isNotEmpty()) {
                val pointer = runBlocking { settingsDataStore.genreRotationPointer.first() }
                val lastDate = runBlocking { settingsDataStore.genreRotationLastDate.first() }
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                if (lastDate != todayStr) {
                    val newPointer = CategoryRotationEngine.advancePointer(
                        pointer, selectedCategories, count
                    )
                    runBlocking {
                        settingsDataStore.setGenreRotationPointer(newPointer)
                        settingsDataStore.setGenreRotationLastDate(todayStr)
                    }
                    Log.d(TAG, "[RECEIVE] Pointer advanced to $newPointer for date $todayStr")
                } else {
                    Log.d(TAG, "[RECEIVE] Pointer already advanced today ($todayStr). Skipping duplicate advance.")
                }
            }

            // Always reschedule to set up next day's alarm
            Log.d(TAG, "[RECEIVE] Rescheduling for next day...")
            QuoteNotificationScheduler.rescheduleAllQuoteNotifications(context)
        } else {
            Log.w(TAG, "[RECEIVE] Unknown action: $action — ignoring.")
        }
    }
}
