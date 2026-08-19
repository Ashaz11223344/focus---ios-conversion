package com.focus.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.focus.database.FocusDatabase
import com.focus.model.Quote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface QuoteRepository {
    val allFavorites: Flow<List<Quote>>
    val recentHistory: Flow<List<Quote>>
    suspend fun addFavorite(quote: Quote)
    suspend fun removeFavorite(quote: Quote)
    fun isFavorite(quoteText: String): Flow<Boolean>
    suspend fun addToHistory(quote: Quote)
    suspend fun clearHistory()
    suspend fun deleteHistoryByText(quoteText: String)
    suspend fun getAllFavoritesDirect(): List<Quote>
    suspend fun getAllHistoryDirect(): List<Quote>
    suspend fun insertFavoritesDirect(quotes: List<Quote>)
    suspend fun insertHistoryDirect(quotes: List<Quote>)
    suspend fun deleteAllFavorites()
    suspend fun deleteAllHistory()

    // Bundled quote dataset methods
    fun initializeQuotes(jsonString: String)
    fun isDatasetLoaded(): Boolean
    fun getAllQuotes(): List<Quote>
    fun getCategories(): List<String>
    fun getRandomQuote(): Quote
    fun getQuotesByCategory(category: String): List<Quote>
    fun searchQuotes(query: String): List<Quote>
    fun getRandomQuoteByCategory(category: String): Quote
}

class SqlDelightQuoteRepository(
    private val database: FocusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : QuoteRepository {
    private val queries = database.focusDatabaseQueries
    private var inMemoryQuotes: List<Quote> = emptyList()
    private var isInitialized = false

    private val englishCommonWords = setOf(
        "the", "and", "of", "to", "a", "is", "that", "it", "in", "you", "was", "for", "on", "are", "as", "with", "his", "they", "i", "at", "be", "this", "have", "from", "or", "one", "had", "by", "but", "not", "what", "all", "were", "we", "when", "your", "can", "there", "an", "each", "which", "she", "do", "how", "their", "if", "will", "up", "about", "out", "them", "these", "so", "some", "her", "would", "like", "him", "into", "has", "more", "go", "see", "no", "way", "could", "people", "my", "than", "who", "its", "now", "find", "day", "get", "come", "make", "us"
    )

    override val allFavorites: Flow<List<Quote>> = queries.selectAllFavorites()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map { Quote(it.text, it.category) } }

    override val recentHistory: Flow<List<Quote>> = queries.selectRecentHistory()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map { Quote(it.text, it.category) } }

    override suspend fun addFavorite(quote: Quote) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.insertFavorite(quote.text, quote.category, now)
    }

    override suspend fun removeFavorite(quote: Quote) = withContext(ioDispatcher) {
        queries.deleteFavorite(quote.text)
    }

    override fun isFavorite(quoteText: String): Flow<Boolean> {
        return queries.isFavorite(quoteText)
            .asFlow()
            .mapToOne(ioDispatcher)
    }

    override suspend fun addToHistory(quote: Quote) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.deleteHistoryByText(quote.text)
        queries.insertHistory(null, quote.text, quote.category, now)
    }

    override suspend fun clearHistory() = withContext(ioDispatcher) {
        queries.clearHistory()
    }

    override suspend fun deleteHistoryByText(quoteText: String) = withContext(ioDispatcher) {
        queries.deleteHistoryByText(quoteText)
    }

    override suspend fun getAllFavoritesDirect(): List<Quote> = withContext(ioDispatcher) {
        queries.selectAllFavorites().executeAsList().map { Quote(it.text, it.category) }
    }

    override suspend fun getAllHistoryDirect(): List<Quote> = withContext(ioDispatcher) {
        queries.selectAllHistory().executeAsList().map { Quote(it.text, it.category) }
    }

    override suspend fun insertFavoritesDirect(quotes: List<Quote>) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        database.transaction {
            quotes.forEach {
                queries.insertFavorite(it.text, it.category, now)
            }
        }
    }

    override suspend fun insertHistoryDirect(quotes: List<Quote>) = withContext(ioDispatcher) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        database.transaction {
            quotes.forEach {
                queries.insertHistory(null, it.text, it.category, now)
            }
        }
    }

    override suspend fun deleteAllFavorites() = withContext(ioDispatcher) {
        queries.deleteAllFavorites()
    }

    override suspend fun deleteAllHistory() = withContext(ioDispatcher) {
        queries.clearHistory()
    }

    // --- In-Memory Dataset Operations ---

    override fun initializeQuotes(jsonString: String) {
        if (isInitialized) return
        try {
            val jsonElement = Json.parseToJsonElement(jsonString).jsonArray
            val loaded = mutableListOf<Quote>()
            for (element in jsonElement) {
                val obj = element.jsonObject
                val text = obj["text"]?.jsonPrimitive?.content ?: ""
                val category = obj["category"]?.jsonPrimitive?.content ?: ""
                if (text.isNotBlank() && isEnglish(text)) {
                    loaded.add(Quote(text, category))
                }
            }
            inMemoryQuotes = loaded
            isInitialized = true
        } catch (e: Exception) {
            inMemoryQuotes = listOf(
                Quote("Believe you can and you're halfway there.", "Motivation"),
                Quote("The best way to predict the future is to create it.", "Motivation")
            )
            isInitialized = true
        }
    }

    override fun isDatasetLoaded(): Boolean = isInitialized

    override fun getAllQuotes(): List<Quote> = inMemoryQuotes

    override fun getCategories(): List<String> {
        return inMemoryQuotes.map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .map { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
            .distinct()
            .sorted()
    }

    override fun getRandomQuote(): Quote {
        return inMemoryQuotes.randomOrNull() ?: Quote("Stay motivated and focused!", "Motivation")
    }

    override fun getQuotesByCategory(category: String): List<Quote> {
        return inMemoryQuotes.filter { it.category.equals(category, ignoreCase = true) }
    }

    override fun searchQuotes(query: String): List<Quote> {
        if (query.isBlank()) return emptyList()
        return inMemoryQuotes.filter { it.text.contains(query, ignoreCase = true) }
    }

    override fun getRandomQuoteByCategory(category: String): Quote {
        val filtered = getQuotesByCategory(category)
        return filtered.randomOrNull() ?: getRandomQuote()
    }

    private fun isEnglish(text: String): Boolean {
        for (char in text) {
            val code = char.code
            if (code in 0x0400..0x04FF || code in 0x0600..0x06FF || code in 0x0900..0x1CFF || code >= 0x2E80) {
                return false
            }
        }
        val words = text.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (words.size <= 2 && words.isNotEmpty()) {
            return text.all { it.code < 128 }
        }
        return words.any { it in englishCommonWords }
    }
}
