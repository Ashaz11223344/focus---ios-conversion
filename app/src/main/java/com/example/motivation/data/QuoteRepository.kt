package com.example.motivation.data
import com.example.motivation.model.Quote

object QuoteRepository {
    private var quotes: List<Quote> = emptyList()
    private var isInitialized = false

    fun initialize(context: android.content.Context) {
        if (isInitialized) return
        try {
            val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val loadedQuotes = mutableListOf<Quote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loadedQuotes.add(Quote(obj.getString("text"), obj.getString("category")))
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
