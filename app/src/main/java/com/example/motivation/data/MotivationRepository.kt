package com.example.motivation.data

import com.example.motivation.data.local.*
import com.example.motivation.model.JournalEntry
import com.example.motivation.model.Quote
import com.example.motivation.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MotivationRepository(private val dao: MotivationDao) {

    // Journal
    val allJournalEntries: Flow<List<JournalEntry>> = dao.getAllJournalEntries().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun insertJournalEntry(content: String, dateDisplay: String) {
        val entity = JournalEntryEntity(
            id = java.util.UUID.randomUUID().toString(),
            content = content,
            timestamp = System.currentTimeMillis(),
            dateDisplay = dateDisplay
        )
        dao.insertJournalEntry(entity)
    }

    // Mood
    val allMoodEntries: Flow<List<MoodEntry>> = dao.getAllMoodEntries().map { entities ->
        entities.map { it.toModel() }
    }

    suspend fun insertMoodEntry(name: String, emoji: String, value: Int, id: Int = 0) {
        dao.insertMoodEntry(MoodEntryEntity(id = id, moodName = name, moodEmoji = emoji, moodValue = value))
    }

    // Favorites
    val allFavorites: Flow<List<Quote>> = dao.getAllFavorites().map { entities ->
        entities.map { Quote(it.text, it.category) }
    }

    suspend fun toggleFavorite(quote: Quote) {
        val entity = FavoriteQuoteEntity(quote.text, quote.category)
        // Note: Simple toggle logic should ideally check existence first if not handled by DAO
        // But for simplicity, we can let ViewModel handle the check or use a custom query
        // Here we'll just provide insert/delete
    }

    suspend fun addFavorite(quote: Quote) {
        dao.insertFavorite(FavoriteQuoteEntity(quote.text, quote.category))
    }

    suspend fun removeFavorite(quote: Quote) {
        dao.deleteFavorite(FavoriteQuoteEntity(quote.text, quote.category))
    }

    fun isFavorite(quoteText: String): Flow<Boolean> = dao.isFavorite(quoteText)

    // History
    val recentHistory: Flow<List<Quote>> = dao.getRecentHistory().map { entities ->
        entities.map { Quote(it.text, it.category) }
    }

    suspend fun addToHistory(quote: Quote) {
        // Optional: delete old one to move it to top
        dao.deleteHistoryByText(quote.text)
        dao.insertHistory(QuoteHistoryEntity(text = quote.text, category = quote.category))
    }

    // Mapper extensions
    private fun JournalEntryEntity.toModel() = JournalEntry(id, content, timestamp, dateDisplay)
    private fun MoodEntryEntity.toModel() = MoodEntry(id, moodName, moodEmoji, moodValue, timestamp)
}
