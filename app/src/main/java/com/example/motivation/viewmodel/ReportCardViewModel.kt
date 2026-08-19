package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.MotivationRepository
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.data.local.AppDatabase
import com.example.motivation.model.JournalEntry
import com.example.motivation.model.MoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class MoodDayPoint(
    val dayOfWeek: Int, // 1 = Mon, 7 = Sun
    val dayName: String,
    val score: Float?,
    val emoji: String?
)

data class ReportCardUiState(
    val isLoading: Boolean = true,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val formattedDateRange: String = "",
    val isPartialWeek: Boolean = false,
    val moodCurveData: List<MoodDayPoint> = emptyList(),
    val topWords: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val journalEntryCount: Int = 0,
    val avgMoodScore: Double = 0.0,
    val avgMoodEmoji: String = "",
    val avgMoodLabel: String = "",
    val hasMoodLogs: Boolean = false
)

class ReportCardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MotivationRepository?
    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(ReportCardUiState())
    val uiState: StateFlow<ReportCardUiState> = _uiState.asStateFlow()

    private val stopWords = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren't", "as", "at",
        "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't", "cannot", "could",
        "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for",
        "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's",
        "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'm", "i've", "i'd",
        "i'll", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't",
        "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours",
        "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't",
        "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there",
        "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too",
        "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were", "weren't",
        "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's",
        "with", "won't", "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself",
        "yourselves", "was", "were", "got", "get", "just", "like", "go", "went", "day", "today", "feel", "feeling",
        "really", "one", "can", "will", "would", "should", "could"
    )

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    init {
        var repo: MotivationRepository? = null
        try {
            val database = AppDatabase.getDatabase(application)
            repo = MotivationRepository(database.motivationDao())
        } catch (e: Exception) {
            android.util.Log.e("ReportCardViewModel", "Failed to initialize MotivationRepository", e)
        }
        repository = repo
        
        generateReport(0)
    }

    fun selectPreviousWeek() {
        generateReport(_weekOffset.value + 1)
    }

    fun selectNextWeek() {
        if (_weekOffset.value > 0) {
            generateReport(_weekOffset.value - 1)
        }
    }

    fun generateReport(offset: Int = 0) {
        _weekOffset.value = offset
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis() - (offset * 7L * 24 * 60 * 60 * 1000)
                val startOfWeek = getStartOfWeek(now)
                val endOfWeek = getEndOfWeek(startOfWeek)
                
                val isPartial = if (offset == 0) {
                    val currentDayOfWeek = Calendar.getInstance(Locale.getDefault()).get(Calendar.DAY_OF_WEEK)
                    currentDayOfWeek != Calendar.SUNDAY
                } else {
                    false
                }

                // Format date range
                val startCal = Calendar.getInstance().apply { timeInMillis = startOfWeek }
                val endCal = Calendar.getInstance().apply { timeInMillis = endOfWeek }
                val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val endFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val formattedRange = "${startFormat.format(startCal.time)} – ${endFormat.format(endCal.time)}"

                // Fetch database stats
                val rawMoodLogs = repository?.getMoodLogsBetween(startOfWeek, endOfWeek) ?: emptyList()
                val journalEntries = repository?.getJournalEntriesBetween(startOfWeek, endOfWeek) ?: emptyList()
                val privateJournalDao = AppDatabase.getDatabase(getApplication()).privateJournalDao()
                val privateJournalEntries = privateJournalDao.getPrivateEntriesBetween(startOfWeek, endOfWeek)
                val streakCount = settingsDataStore.streakCount.first()

                // Deduplicate mood logs by calendar day (keeping only the latest entry per calendar day)
                val moodLogs = rawMoodLogs.groupBy { entry ->
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
                }.mapValues { (_, dayEntries) ->
                    dayEntries.maxByOrNull { it.timestamp }!!
                }.values.toList()

                // 1. Mood Curve Data (Mon to Sun)
                val moodCurveData = (0..6).map { dayIdx ->
                    val dayName = when (dayIdx) {
                        0 -> "Mon"
                        1 -> "Tue"
                        2 -> "Wed"
                        3 -> "Thu"
                        4 -> "Fri"
                        5 -> "Sat"
                        else -> "Sun"
                    }
                    val dayStart = startOfWeek + dayIdx * 86400000L
                    val dayEnd = dayStart + 86400000L - 1
                    
                    val dayLogs = moodLogs.filter { it.timestamp in dayStart..dayEnd }
                    val dayJournals = journalEntries.filter { it.timestamp in dayStart..dayEnd }
                    val dayPrivateJournals = privateJournalEntries.filter { it.timestamp in dayStart..dayEnd }
                    
                    if (dayLogs.isEmpty() && dayJournals.isEmpty() && dayPrivateJournals.isEmpty()) {
                        MoodDayPoint(dayIdx + 1, dayName, null, null)
                    } else if (dayLogs.isNotEmpty()) {
                        val avgScore = dayLogs.map { it.moodValue }.average().toFloat()
                        val latestLog = dayLogs.maxByOrNull { it.timestamp }
                        MoodDayPoint(dayIdx + 1, dayName, avgScore, getEmojiChar(latestLog?.moodName))
                    } else {
                        // Journal entry exists but no mood log -> represent as Neutral (3.0)
                        MoodDayPoint(dayIdx + 1, dayName, 3.0f, "😐")
                    }
                }

                // 2. Word Frequency Analytics
                val wordsList = journalEntries.flatMap { entry ->
                    entry.content.lowercase(Locale.ROOT)
                        .split(Regex("[^a-zA-Z]+"))
                        .filter { it.isNotBlank() && it.length >= 3 && !stopWords.contains(it) }
                }
                
                val topWords = wordsList.groupBy { it }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    .map { it.first }

                // 3. Average Mood score and details
                val combinedScores = (0..6).mapNotNull { dayIdx ->
                    val dayStart = startOfWeek + dayIdx * 86400000L
                    val dayEnd = dayStart + 86400000L - 1
                    
                    val dayLogs = moodLogs.filter { it.timestamp in dayStart..dayEnd }
                    val dayJournals = journalEntries.filter { it.timestamp in dayStart..dayEnd }
                    val dayPrivateJournals = privateJournalEntries.filter { it.timestamp in dayStart..dayEnd }
                    
                    if (dayLogs.isNotEmpty()) {
                        dayLogs.map { it.moodValue }.average()
                    } else if (dayJournals.isNotEmpty() || dayPrivateJournals.isNotEmpty()) {
                        3.0 // Neutral score for journal entries
                    } else {
                        null
                    }
                }

                val hasLogs = combinedScores.isNotEmpty()
                val avgScore = if (hasLogs) combinedScores.average() else 0.0
                
                val (emoji, label) = when {
                    !hasLogs -> Pair("🌙", "No Logs")
                    avgScore >= 4.5 -> Pair("😊", "Amazing")
                    avgScore >= 3.5 -> Pair("😌", "Good")
                    avgScore >= 2.5 -> Pair("😐", "Neutral")
                    avgScore >= 1.5 -> Pair("😔", "Bad")
                    else -> Pair("😡", "Awful")
                }

                _uiState.value = ReportCardUiState(
                    isLoading = false,
                    startDate = startOfWeek,
                    endDate = endOfWeek,
                    formattedDateRange = formattedRange,
                    isPartialWeek = isPartial,
                    moodCurveData = moodCurveData,
                    topWords = topWords,
                    currentStreak = streakCount,
                    journalEntryCount = journalEntries.size + privateJournalEntries.size,
                    avgMoodScore = avgScore,
                    avgMoodEmoji = emoji,
                    avgMoodLabel = label,
                    hasMoodLogs = hasLogs
                )
            }
        }
    }

    private fun getStartOfWeek(timeMs: Long): Long {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = timeMs
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert SUNDAY=1, MONDAY=2, ... to subtraction offsets
        val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfWeek(startOfWeekMs: Long): Long {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = startOfWeekMs
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getEmojiChar(moodName: String?): String {
        return when (moodName?.lowercase(Locale.ROOT)) {
            "happy" -> "😊"
            "inspired" -> "✨"
            "calm" -> "🧘"
            "neutral" -> "😐"
            "sad" -> "😔"
            "tired" -> "😴"
            "angry" -> "😡"
            "anxious" -> "🥺"
            else -> "✦"
        }
    }
}
