package com.example.motivation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.local.AppDatabase
import com.example.motivation.data.local.PrivateJournalEntry
import com.example.motivation.security.PinManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrivateJournalViewModel(application: Application) : AndroidViewModel(application) {

    private val privateDao = AppDatabase.getDatabase(application).privateJournalDao()

    // Flow of private entries
    val privateEntries: StateFlow<List<PrivateJournalEntry>> = privateDao.getAllPrivateEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unlocked state
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Failed attempts tracking
    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    // Lockout tracking
    private val _lockoutEndTime = MutableStateFlow<Long?>(null)
    val lockoutEndTime: StateFlow<Long?> = _lockoutEndTime.asStateFlow()

    private val _lockoutTimeRemainingSecs = MutableStateFlow(0L)
    val lockoutTimeRemainingSecs: StateFlow<Long> = _lockoutTimeRemainingSecs.asStateFlow()

    private var countdownJob: Job? = null

    fun unlock() {
        _isUnlocked.value = true
        resetAttempts()
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun getFailedAttempts(): Int = _failedAttempts.value

    fun recordFailedAttempt() {
        val nextAttempts = _failedAttempts.value + 1
        _failedAttempts.value = nextAttempts
        if (nextAttempts >= 5) {
            val endTime = System.currentTimeMillis() + 30_000L
            _lockoutEndTime.value = endTime
            startLockoutCountdown(endTime)
        }
    }

    fun resetAttempts() {
        _failedAttempts.value = 0
        _lockoutEndTime.value = null
        _lockoutTimeRemainingSecs.value = 0L
        countdownJob?.cancel()
    }

    fun isLockedOut(): Boolean {
        val endTime = _lockoutEndTime.value ?: return false
        if (System.currentTimeMillis() >= endTime) {
            resetAttempts()
            return false
        }
        return true
    }

    private fun startLockoutCountdown(endTime: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = endTime - now
                if (diff <= 0) {
                    resetAttempts()
                    break
                }
                _lockoutTimeRemainingSecs.value = (diff + 999L) / 1000L
                delay(500L)
            }
        }
    }

    // Insert Entry
    fun insertPrivateEntry(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val words = content.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            val entry = PrivateJournalEntry(
                content = content,
                timestamp = System.currentTimeMillis(),
                wordCount = words.size
            )
            privateDao.insertPrivateEntry(entry)
            com.example.motivation.data.AchievementRepository(getApplication()).checkAchievements()
        }
    }

    // Delete Entry
    fun deletePrivateEntry(entry: PrivateJournalEntry) {
        viewModelScope.launch {
            privateDao.deletePrivateEntry(entry)
            com.example.motivation.data.AchievementRepository(getApplication()).checkAchievements()
        }
    }

    // Delete All (Reset PIN flow)
    fun resetAndDeleteAllPrivateData(context: Context) {
        viewModelScope.launch {
            privateDao.deleteAllPrivateEntries()
            PinManager.clearPin(context)
            lock()
            resetAttempts()
        }
    }
}
