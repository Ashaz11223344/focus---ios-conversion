package com.example.motivation.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class UserProfileRepository(private val context: Context) {
    private val settingsDataStore = SettingsDataStore(context)

    val userName: Flow<String> = settingsDataStore.userName
    val userId: Flow<String> = settingsDataStore.userId
    val userProfilePhotoUri: Flow<String?> = settingsDataStore.userProfilePhotoUri
    val enableBadgeDisplay: Flow<Boolean> = settingsDataStore.enableBadgeDisplay
    val profileCreatedDate: Flow<Long> = settingsDataStore.profileCreatedDate

    suspend fun saveProfile(name: String, photoUriString: String?, isUpdate: Boolean = false) {
        settingsDataStore.setUserName(name)
        if (!isUpdate) {
            settingsDataStore.setProfileCreatedDate(System.currentTimeMillis())
        }
        settingsDataStore.ensureUserId()
        
        if (photoUriString != null) {
            savePhotoToCache(photoUriString)
        }
    }

    suspend fun updatePhoto(photoUriString: String?) {
        if (photoUriString != null) {
            savePhotoToCache(photoUriString)
        } else {
            val file = File(context.cacheDir, "profile_photo.jpg")
            if (file.exists()) {
                file.delete()
            }
            settingsDataStore.setUserProfilePhotoUri(null)
        }
    }

    suspend fun setEnableBadgeDisplay(enable: Boolean) {
        settingsDataStore.setEnableBadgeDisplay(enable)
    }

    private suspend fun savePhotoToCache(uriString: String) {
        try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val outFile = File(context.cacheDir, "profile_photo.jpg")
                FileOutputStream(outFile).use { output ->
                    inputStream.copyTo(output)
                }
                settingsDataStore.setUserProfilePhotoUri(outFile.absolutePath)
            }
        } catch (e: Exception) {
            android.util.Log.e("UserProfileRepository", "Error saving photo", e)
        }
    }
}
