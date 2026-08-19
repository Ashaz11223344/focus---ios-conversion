package com.example.motivation.data
import com.example.motivation.model.Quote

object QuoteRepository {
    private var quotes: List<Quote> = emptyList()
    private var isInitialized = false

    private val ENGLISH_COMMON_WORDS = setOf(
        "the", "and", "of", "to", "a", "is", "that", "it", "in", "you", "was", "for", "on", "are", "as", "with", "his", "they", "i", "at", "be", "this", "have", "from", "or", "one", "had", "by", "but", "not", "what", "all", "were", "we", "when", "your", "can", "there", "an", "each", "which", "she", "do", "how", "their", "if", "will", "up", "about", "out", "them", "these", "so", "some", "her", "would", "like", "him", "into", "has", "more", "go", "see", "no", "way", "could", "people", "my", "than", "who", "its", "now", "find", "day", "get", "come", "make", "us"
    )

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
        return words.any { it in ENGLISH_COMMON_WORDS }
    }

    @Synchronized
    fun initialize(context: android.content.Context) {
        if (isInitialized) return
        try {
            val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val loadedQuotes = mutableListOf<Quote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val text = obj.getString("text")
                val category = obj.getString("category")
                if (isEnglish(text)) {
                    loadedQuotes.add(Quote(text, category))
                }
            }
            quotes = loadedQuotes
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to a few default quotes if file not found
            quotes = listOf(
                Quote("Believe you can and you're halfway there.", "Motivation"),
                Quote("The best way to predict the future is to create it.", "Motivation")
            )
        }
    }

    fun getAllQuotes(): List<Quote> {
        return quotes
    }

    fun getCategories(): List<String> {
        return quotes.map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .map { it.lowercase(java.util.Locale.ROOT).replaceFirstChar { char -> char.titlecase(java.util.Locale.ROOT) } }
            .distinct()
            .sorted()
    }

    fun getRandomQuote(): Quote {
        return if (quotes.isNotEmpty()) quotes.random() else Quote("Stay motivated!", "Motivation")
    }

    fun getQuotesByCategory(category: String): List<Quote> {
        return quotes.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun searchQuotes(query: String): List<Quote> {
        if (query.isBlank()) return emptyList()
        return quotes.filter { it.text.contains(query, ignoreCase = true) }
    }

    fun getRandomQuoteByCategory(category: String): Quote {
        val filtered = getQuotesByCategory(category)
        return if (filtered.isNotEmpty()) filtered.random() else getRandomQuote()
    }
}
