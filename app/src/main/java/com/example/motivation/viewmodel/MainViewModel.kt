package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.motivation.data.QuoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Directly get the list of quotes (which are Strings) from the central repository
    private val allQuotes: List<String> = QuoteRepository.allQuotes

    // The state now holds a single String quote, not a complex Quote object
    private val _quote = MutableStateFlow<String?>(null)
    val quote: StateFlow<String?> = _quote

    private var quoteIndex = 0

    init {
        // Initialize with the first quote based on the date
        updateQuoteBasedOnDate()
    }

    /**
     * Sets the daily quote based on the day of the year to ensure a new quote each day.
     */
    private fun updateQuoteBasedOnDate() {
        if (allQuotes.isEmpty()) {
            _quote.value = null
            return
        }
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        quoteIndex = dayOfYear % allQuotes.size
        _quote.value = allQuotes[quoteIndex]
    }

    /**
     * Updates the UI with the next quote in the list, looping back to the start if necessary.
     */
    fun getNextQuote() {
        if (allQuotes.isEmpty()) return
        quoteIndex = (quoteIndex + 1) % allQuotes.size
        _quote.value = allQuotes[quoteIndex]
    }

    /**
     * Updates the UI with the previous quote in the list, looping to the end if necessary.
     */
    fun getPreviousQuote() {
        if (allQuotes.isEmpty()) return
        quoteIndex = (quoteIndex - 1 + allQuotes.size) % allQuotes.size
        _quote.value = allQuotes[quoteIndex]
    }
}
