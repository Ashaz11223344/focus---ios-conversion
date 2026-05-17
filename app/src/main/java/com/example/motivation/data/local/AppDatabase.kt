package com.example.motivation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        JournalEntryEntity::class,
        MoodEntryEntity::class,
        FavoriteQuoteEntity::class,
        QuoteHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun motivationDao(): MotivationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "motivation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
