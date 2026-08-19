package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.AchievementRepository
import com.example.motivation.data.UserProfileRepository
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.data.local.Achievement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)
    private val userProfileRepository = UserProfileRepository(application)
    private val achievementRepository = AchievementRepository(application)

    val userName: StateFlow<String> = userProfileRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userId: StateFlow<String> = userProfileRepository.userId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userProfilePhotoUri: StateFlow<String?> = userProfileRepository.userProfilePhotoUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val enableBadgeDisplay: StateFlow<Boolean> = userProfileRepository.enableBadgeDisplay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val profileCreatedDate: StateFlow<Long> = userProfileRepository.profileCreatedDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val achievements: StateFlow<List<Achievement>> = achievementRepository.allAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streakCount: StateFlow<Int> = settingsDataStore.streakCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            achievementRepository.initDefaultAchievements()
            achievementRepository.checkAchievements()
        }
    }

    fun saveProfile(name: String, photoUriString: String?, isUpdate: Boolean = false) {
        viewModelScope.launch {
            userProfileRepository.saveProfile(name, photoUriString, isUpdate)
            achievementRepository.checkAchievements()
        }
    }

    fun updatePhoto(photoUriString: String?) {
        viewModelScope.launch {
            userProfileRepository.updatePhoto(photoUriString)
            achievementRepository.checkAchievements()
        }
    }

    fun setEnableBadgeDisplay(enable: Boolean) {
        viewModelScope.launch {
            userProfileRepository.setEnableBadgeDisplay(enable)
        }
    }

    fun checkAchievements() {
        viewModelScope.launch {
            achievementRepository.checkAchievements()
        }
    }
}
