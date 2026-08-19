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
            
            // Load saved quote immediately from SharedPreferences (fast, no disk parse)
            val prefs = application.getSharedPreferences("widget_data", android.content.Context.MODE_PRIVATE)
            val savedText = prefs.getString("current_quote_text", null)
            val savedCategory = prefs.getString("current_quote_category", null)
            if (savedText != null && savedCategory != null) {
                _quote.value = Quote(savedText, savedCategory)
            }

            // Move heavy JSON parsing + regex filtering to background IO thread
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                QuoteRepository.initialize(application)
                // If no saved quote was found, refresh from loaded repository
                if (savedText == null || savedCategory == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        refreshQuote()
                    }
                }
            }

            // One-time cleanup of legacy duplicate mood entries
            viewModelScope.launch {
                try {
                    database.motivationDao().deleteDuplicateMoods()
                    android.util.Log.d("MainViewModel", "Legacy duplicate moods cleaned up successfully")
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Failed to clean up duplicate moods", e)
                }
            }
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
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        ?: MutableStateFlow(emptyList())

    private fun deduplicateMoodsByDate(entries: List<MoodEntry>): List<MoodEntry> {
        return entries.groupBy { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }.mapValues { (_, dayEntries) ->
            dayEntries.maxByOrNull { it.timestamp }!!
        }.values.toList()
    }

    val moodEntries: StateFlow<List<MoodEntry>> = repository?.allMoodEntries
        ?.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        ?: MutableStateFlow(emptyList())

    val todayMoodEntry: StateFlow<MoodEntry?> = moodEntries.map { entries ->
        val todayStart = getStartOfDayLocal()
        val todayEnd = getEndOfDayLocal()
        entries.firstOrNull { it.timestamp in todayStart..todayEnd }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Mood Analytics
    val monthlyMoodStats: StateFlow<MoodStats?> = moodEntries.map { rawEntries ->
        val entries = deduplicateMoodsByDate(rawEntries)
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

    private var historyIndex = 0

    fun showNextQuote() {
        val currentHistory = history.value
        if (historyIndex > 0 && historyIndex - 1 < currentHistory.size) {
            historyIndex--
            val nextQuote = currentHistory[historyIndex]
            _quote.value = nextQuote
            updateQuotesWidgets(nextQuote.text, nextQuote.category)
        } else {
            historyIndex = 0
            refreshQuote()
        }
    }

    fun showPreviousQuote() {
        val currentHistory = history.value
        if (historyIndex + 1 < currentHistory.size) {
            historyIndex++
            val prevQuote = currentHistory[historyIndex]
            _quote.value = prevQuote
            updateQuotesWidgets(prevQuote.text, prevQuote.category)
        }
    }

    fun refreshQuote() {
        historyIndex = 0
        val nextQuote = QuoteRepository.getRandomQuote()
        _quote.value = nextQuote
        viewModelScope.launch {
            repository?.addToHistory(nextQuote)
        }
        updateQuotesWidgets(nextQuote.text, nextQuote.category)
    }

    fun setQuote(text: String, category: String) {
        historyIndex = 0
        val nextQuote = Quote(text, category)
        _quote.value = nextQuote
        viewModelScope.launch {
            repository?.addToHistory(nextQuote)
        }
        updateQuotesWidgets(nextQuote.text, nextQuote.category)
    }

    fun refreshQuoteByCategory(category: String) {
        historyIndex = 0
        val nextQuote = QuoteRepository.getRandomQuoteByCategory(category)
        _quote.value = nextQuote
        viewModelScope.launch {
            repository?.addToHistory(nextQuote)
        }
        updateQuotesWidgets(nextQuote.text, nextQuote.category)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository?.clearHistory()
        }
    }

    val categories: List<String> = listOf("All") + QuoteRepository.getCategories()

    private val _searchSuggestions = MutableStateFlow<List<Quote>>(emptyList())
    val searchSuggestions: StateFlow<List<Quote>> = _searchSuggestions.asStateFlow()

    private val suggestedQuotesHistory = mutableSetOf<String>()

    fun generateSuggestions(category: String, limit: Int = 5) {
        val filteredQuotes = if (category.equals("All", ignoreCase = true) || category.isBlank()) {
            QuoteRepository.getAllQuotes()
        } else {
            QuoteRepository.getQuotesByCategory(category)
        }

        var available = filteredQuotes.filter { it.text !in suggestedQuotesHistory }

        if (available.size < limit && filteredQuotes.isNotEmpty()) {
            val textsToRemove = filteredQuotes.map { it.text }.toSet()
            suggestedQuotesHistory.removeAll(textsToRemove)
            available = filteredQuotes
        }

        val selected = available.shuffled().take(limit)
        suggestedQuotesHistory.addAll(selected.map { it.text })
        _searchSuggestions.value = selected
    }

    fun search(query: String, category: String = "All") {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        val rawResults = QuoteRepository.searchQuotes(query)
        _searchResults.value = if (category.equals("All", ignoreCase = true)) {
            rawResults
        } else {
            rawResults.filter { it.category.equals(category, ignoreCase = true) }
        }
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
            updateJournalWidgets()
            com.example.motivation.data.AchievementRepository(getApplication()).checkAchievements()
        }
    }

    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository?.deleteJournalEntry(entry)
            updateJournalWidgets()
            com.example.motivation.data.AchievementRepository(getApplication()).checkAchievements()
        }
    }

    fun reinsertJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository?.insertJournalEntryDirect(entry)
            updateJournalWidgets()
            com.example.motivation.data.AchievementRepository(getApplication()).checkAchievements()
        }
    }

    fun updateJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository?.updateJournalEntry(entry)
            updateJournalWidgets()
        }
    }

    fun addMoodEntry(name: String, emoji: String, value: Int, onComplete: (isUpdate: Boolean) -> Unit) {
        viewModelScope.launch {
            val startOfDay = getStartOfDayLocal()
            val endOfDay = getEndOfDayLocal()
            
            val existing = repository?.getMoodEntryForDayRange(startOfDay, endOfDay)
            val isUpdate = existing != null
            val idToUse = existing?.id ?: 0
            
            repository?.insertMoodEntry(
                name = name,
                emoji = emoji,
                value = value,
                id = idToUse
            )
            com.example.motivation.data.AchievementRepository(getApplication()).checkAchievements()
            onComplete(isUpdate)
        }
    }

    private fun getStartOfDayLocal(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDayLocal(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun updateQuotesWidgets(quoteText: String, quoteCategory: String) {
        val context = getApplication<android.app.Application>().applicationContext
        val prefs = context.getSharedPreferences("widget_data", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("current_quote_text", quoteText)
            .putString("current_quote_category", quoteCategory)
            .apply()

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)

        // Medium Widget Broadcast
        val mediumIntent = android.content.Intent(context, com.example.motivation.widget.QuotesMediumWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val mediumIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, com.example.motivation.widget.QuotesMediumWidgetProvider::class.java)
        )
        mediumIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, mediumIds)
        context.sendBroadcast(mediumIntent)
    }

    private fun updateJournalWidgets() {
        val context = getApplication<android.app.Application>().applicationContext
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)

        // Small Journal Widget Broadcast
        val smallIntent = android.content.Intent(context, com.example.motivation.widget.JournalSmallWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val smallIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, com.example.motivation.widget.JournalSmallWidgetProvider::class.java)
        )
        smallIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, smallIds)
        context.sendBroadcast(smallIntent)

        // Medium Journal Widget Broadcast
        val mediumIntent = android.content.Intent(context, com.example.motivation.widget.JournalMediumWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val mediumIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, com.example.motivation.widget.JournalMediumWidgetProvider::class.java)
        )
        context.sendBroadcast(mediumIntent)
    }

    fun favoriteQuotes(quotes: List<Quote>) {
        viewModelScope.launch {
            quotes.forEach { quote ->
                if (!favorites.value.any { it.text == quote.text }) {
                    repository?.addFavorite(quote)
                }
            }
        }
    }

    fun removeFavorites(quotes: List<Quote>) {
        viewModelScope.launch {
            quotes.forEach { quote ->
                repository?.removeFavorite(quote)
            }
        }
    }

    fun removeHistory(quotes: List<Quote>) {
        viewModelScope.launch {
            quotes.forEach { quote ->
                repository?.deleteHistoryByText(quote.text)
            }
        }
    }

    fun reinsertHistory(quotes: List<Quote>) {
        viewModelScope.launch {
            quotes.forEach { quote ->
                repository?.addToHistory(quote)
            }
        }
    }
}

data class MoodStats(
    val averageScore: Double,
    val mostFrequentMood: String,
    val count: Int
)
