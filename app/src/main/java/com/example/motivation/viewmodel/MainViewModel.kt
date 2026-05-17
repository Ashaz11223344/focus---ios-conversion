package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.QuoteRepository
import com.example.motivation.data.MotivationRepository
import com.example.motivation.data.local.AppDatabase
import com.example.motivation.model.Quote
import com.example.motivation.model.JournalEntry
import com.example.motivation.model.MoodEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MotivationRepository?
    private val _quote = MutableStateFlow(Quote("Focus on your goals.", "Focus"))
    val quote: StateFlow<Quote> = _quote.asStateFlow()

    init {
        var repo: MotivationRepository? = null
        try {
            android.util.Log.d("MainViewModel", "Initializing Database...")
            val database = AppDatabase.getDatabase(application)
            repo = MotivationRepository(database.motivationDao())
            android.util.Log.d("MainViewModel", "Database Initialized Successfully")
            
            QuoteRepository.initialize(application)
            refreshQuote()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to initialize Database or Repository", e)
        }
        repository = repo
    }

    private val _searchResults = MutableStateFlow<List<Quote>>(emptyList())
    val searchResults: StateFlow<List<Quote>> = _searchResults.asStateFlow()

    // Observe data from repository safely
    val journalEntries: StateFlow<List<JournalEntry>> = repository?.allJournalEntries
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

    val favorites: StateFlow<List<Quote>> = repository?.allFavorites
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

    val history: StateFlow<List<Quote>> = repository?.recentHistory
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

    val moodEntries: StateFlow<List<MoodEntry>> = repository?.allMoodEntries
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

    // Mood Analytics
    val monthlyMoodStats: StateFlow<MoodStats?> = moodEntries.map { entries ->
        if (entries.isEmpty()) return@map null
        
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        
        val thisMonthEntries = entries.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
        
        if (thisMonthEntries.isEmpty()) return@map null
        
        val avgScore = thisMonthEntries.map { it.moodValue }.average()
        val mostFrequent = thisMonthEntries.groupBy { it.moodName }
            .maxByOrNull { it.value.size }?.key ?: "Unknown"
            
        MoodStats(
            averageScore = avgScore,
            mostFrequentMood = mostFrequent,
            count = thisMonthEntries.size
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun refreshQuote() {
        val nextQuote = QuoteRepository.getRandomQuote()
        _quote.value = nextQuote
        viewModelScope.launch {
            repository?.addToHistory(nextQuote)
        }
    }

    fun refreshQuoteByCategory(category: String) {
        val nextQuote = QuoteRepository.getRandomQuoteByCategory(category)
        _quote.value = nextQuote
        viewModelScope.launch {
            repository?.addToHistory(nextQuote)
        }
    }

    fun search(query: String) {
        _searchResults.value = QuoteRepository.searchQuotes(query)
    }

    fun toggleFavorite(quote: Quote) {
        viewModelScope.launch {
            val currentFavorites = favorites.value
            if (currentFavorites.any { it.text == quote.text }) {
                repository?.removeFavorite(quote)
            } else {
                repository?.addFavorite(quote)
            }
        }
    }

    fun addJournalEntry(content: String) {
        if (content.isBlank()) return
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateDisplay = dateFormat.format(Date())
        viewModelScope.launch {
            repository?.insertJournalEntry(content, dateDisplay)
        }
    }

    fun addMoodEntry(name: String, emoji: String, value: Int) {
        viewModelScope.launch {
            val latest = moodEntries.value.firstOrNull()
            val today = System.currentTimeMillis()
            
            val idToUse = if (latest != null && isSameDay(latest.timestamp, today)) {
                latest.id
            } else {
                0
            }
            
            repository?.insertMoodEntry(name, emoji, value, idToUse)
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

data class MoodStats(
    val averageScore: Double,
    val mostFrequentMood: String,
    val count: Int
)
