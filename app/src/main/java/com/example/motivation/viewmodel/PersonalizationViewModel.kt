package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonalizationViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    val userName = settingsDataStore.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveUserName(name: String) {
        viewModelScope.launch {
            settingsDataStore.setUserName(name)
        }
    }
}