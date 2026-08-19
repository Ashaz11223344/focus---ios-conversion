package com.example.motivation.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.motivation.data.local.*
import com.example.motivation.model.BackupPayload
import com.example.motivation.model.SettingsBackup
import com.example.motivation.security.BackupCryptoEngine
import com.example.motivation.security.PinManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BackupRestoreManager(private val context: Context) {

    private val gson = Gson()
    private val database = AppDatabase.getDatabase(context)
    private val settingsDataStore = SettingsDataStore(context)

    /**
     * Aggregates all Room tables and settings, encrypts, and writes to the SAF Uri destination.
     */
    suspend fun exportBackup(uri: Uri, password: CharArray): Unit = withContext(Dispatchers.IO) {
        val motivationDao = database.motivationDao()
        val privateJournalDao = database.privateJournalDao()
        val focusGuardDao = database.focusGuardDao()
        val achievementDao = database.achievementDao()
        val focusSessionDao = database.focusSessionDao()

        // 1. Gather all database records
        val journalEntries = motivationDao.getAllJournalEntriesDirect()
        val moodEntries = motivationDao.getAllMoodEntriesDirect()
        val favorites = motivationDao.getAllFavoritesDirect()
        val history = motivationDao.getAllHistoryDirect()
        val privateJournalEntries = privateJournalDao.getAllPrivateEntriesDirect()
        val dndSchedules = focusGuardDao.getAllDndSchedulesDirect()
        val appBlockRules = focusGuardDao.getAllAppBlockRulesDirect()
        val achievements = achievementDao.getAllAchievementsDirect()
        val focusSessions = focusSessionDao.getAllSessionsDirect()

        // 2. Gather DataStore Settings
        val dataStoreSettings = settingsDataStore.getSettingsForBackup()

        // 3. Gather SharedPreferences and Zen PIN Settings
        val motivationPrefs = context.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
        val onboardingCompleted = motivationPrefs.getBoolean("onboarding_completed", false)
        val onboardingVersion = motivationPrefs.getInt("onboarding_version", 0)
        val quickWallpaperOnHold = motivationPrefs.getBoolean("quick_wallpaper_on_hold", true)

        val pinHash = PinManager.getPinHash(context)
        val biometricEnabled = PinManager.isBiometricEnabled(context)

        // Read profile photo cache and encode to Base64
        val photoFile = java.io.File(context.cacheDir, "profile_photo.jpg")
        val photoBase64 = if (photoFile.exists()) {
            try {
                val bytes = photoFile.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        // Assemble unified SettingsBackup
        val settingsBackup = dataStoreSettings.copy(
            onboardingCompleted = onboardingCompleted,
            onboardingVersion = onboardingVersion,
            quickWallpaperOnHold = quickWallpaperOnHold,
            pinHash = pinHash,
            biometricEnabled = biometricEnabled,
            profilePhotoBase64 = photoBase64
        )

        // 4. Assemble complete payload
        val payload = BackupPayload(
            journalEntries = journalEntries,
            privateJournalEntries = privateJournalEntries,
            moodEntries = moodEntries,
            favorites = favorites,
            history = history,
            dndSchedules = dndSchedules,
            appBlockRules = appBlockRules,
            achievements = achievements,
            focusSessions = focusSessions,
            settings = settingsBackup
        )

        // 5. Serialize payload to JSON and convert to bytes
        val jsonString = gson.toJson(payload)
        val plainBytes = jsonString.toByteArray(Charsets.UTF_8)

        // 6. Encrypt plaintext bytes using the password
        val encryptedBytes = BackupCryptoEngine.encrypt(plainBytes, password)

        // 7. Write encrypted stream to SAF target destination
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Unable to open output stream for backup.")
        outputStream.use { it.write(encryptedBytes) }
    }

    /**
     * Reads, decrypts, and restores all database records and user configurations.
     */
    suspend fun importBackup(uri: Uri, password: CharArray): Unit = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open input stream for restore.")
        
        // 1. Read all bytes from file
        val encryptedBytes = inputStream.use { it.readBytes() }
        if (encryptedBytes.isEmpty()) {
            throw IOException("Selected backup file is empty.")
        }

        // 2. Decrypt payload
        val plainBytes = try {
            BackupCryptoEngine.decrypt(encryptedBytes, password)
        } catch (e: Exception) {
            throw IllegalArgumentException("Incorrect backup password or corrupted backup file.", e)
        }

        // 3. Parse JSON to BackupPayload
        val jsonString = String(plainBytes, Charsets.UTF_8)
        val payload = gson.fromJson(jsonString, BackupPayload::class.java) 
            ?: throw IllegalArgumentException("Parsed payload is null.")

        // 4. Restore Room database inside a single atomic transaction
        database.withTransaction {
            val motivationDao = database.motivationDao()
            val privateJournalDao = database.privateJournalDao()
            val focusGuardDao = database.focusGuardDao()
            val achievementDao = database.achievementDao()
            val focusSessionDao = database.focusSessionDao()

            // Safe clearing
            motivationDao.deleteAllJournalEntries()
            motivationDao.deleteAllMoodEntries()
            motivationDao.deleteAllFavorites()
            motivationDao.deleteAllHistory()
            privateJournalDao.deleteAllPrivateEntries()
            focusGuardDao.deleteAllDndSchedules()
            focusGuardDao.deleteAllAppBlockRules()
            achievementDao.deleteAllAchievements()
            focusSessionDao.deleteAllSessions()

            // Safe restoring
            motivationDao.insertJournalEntries(payload.journalEntries)
            motivationDao.insertMoodEntries(payload.moodEntries)
            motivationDao.insertFavorites(payload.favorites)
            motivationDao.insertHistory(payload.history)
            privateJournalDao.insertPrivateEntries(payload.privateJournalEntries)
            focusGuardDao.insertDndSchedules(payload.dndSchedules)
            focusGuardDao.insertAppBlockRules(payload.appBlockRules)
            
            if (payload.achievements != null) {
                achievementDao.insertAchievements(payload.achievements)
            }
            if (payload.focusSessions != null) {
                focusSessionDao.insertSessions(payload.focusSessions)
            }
        }

        // 5. Restore Preference DataStore
        settingsDataStore.restoreSettings(payload.settings)

        // Restore Profile Photo from base64
        val photoFile = java.io.File(context.cacheDir, "profile_photo.jpg")
        val base64Photo = payload.settings.profilePhotoBase64
        if (!base64Photo.isNullOrBlank()) {
            try {
                val decodedBytes = android.util.Base64.decode(base64Photo, android.util.Base64.NO_WRAP)
                photoFile.writeBytes(decodedBytes)
                settingsDataStore.setUserProfilePhotoUri(photoFile.absolutePath)
            } catch (e: Exception) {
                if (photoFile.exists()) {
                    photoFile.delete()
                }
                settingsDataStore.setUserProfilePhotoUri(null)
            }
        } else {
            if (photoFile.exists()) {
                photoFile.delete()
            }
            settingsDataStore.setUserProfilePhotoUri(null)
        }

        // 6. Restore SharedPreferences
        val motivationPrefs = context.getSharedPreferences("motivation_prefs", Context.MODE_PRIVATE)
        motivationPrefs.edit()
            .putBoolean("onboarding_completed", payload.settings.onboardingCompleted)
            .putInt("onboarding_version", payload.settings.onboardingVersion)
            .putBoolean("quick_wallpaper_on_hold", payload.settings.quickWallpaperOnHold)
            .apply()

        // 7. Restore Secure PIN
        PinManager.setPinHash(context, payload.settings.pinHash)
        PinManager.setBiometricEnabled(context, payload.settings.biometricEnabled)
    }
}
