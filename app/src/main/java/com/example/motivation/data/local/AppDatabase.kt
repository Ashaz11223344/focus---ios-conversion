    package com.example.motivation.data.local

    import android.content.Context
    import androidx.room.Database
    import androidx.room.Room
    import androidx.room.RoomDatabase
    import androidx.room.migration.Migration
    import androidx.sqlite.db.SupportSQLiteDatabase

    @Database(
        entities = [
            JournalEntryEntity::class,
            MoodEntryEntity::class,
            FavoriteQuoteEntity::class,
            QuoteHistoryEntity::class,
            PrivateJournalEntry::class,
            DndScheduleEntity::class,
            AppBlockRuleEntity::class,
            Achievement::class,
            FocusSessionEntity::class
        ],
        version = 6,
        exportSchema = false
    )
    abstract class AppDatabase : RoomDatabase() {
        abstract fun motivationDao(): MotivationDao
        abstract fun privateJournalDao(): PrivateJournalDao
        abstract fun focusGuardDao(): FocusGuardDao
        abstract fun achievementDao(): AchievementDao
        abstract fun focusSessionDao(): FocusSessionDao

        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS affirmations")
                }
            }

            val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE journal_entries ADD COLUMN updatedAt INTEGER")
                }
            }

            val MIGRATION_4_5 = object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `dnd_schedules` (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, label TEXT NOT NULL, startHour INTEGER NOT NULL, startMinute INTEGER NOT NULL, endHour INTEGER NOT NULL, endMinute INTEGER NOT NULL, daysOfWeek INTEGER NOT NULL, isEnabled INTEGER NOT NULL DEFAULT 1)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `app_block_rules` (packageName TEXT PRIMARY KEY NOT NULL, appName TEXT NOT NULL, startHour INTEGER NOT NULL, startMinute INTEGER NOT NULL, endHour INTEGER NOT NULL, endMinute INTEGER NOT NULL, daysOfWeek INTEGER NOT NULL, isEnabled INTEGER NOT NULL DEFAULT 1)")
                }
            }

            val MIGRATION_5_6 = object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `achievements` (`achievementId` TEXT PRIMARY KEY NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `iconEmoji` TEXT NOT NULL, `tier` TEXT NOT NULL, `tierColor` TEXT NOT NULL, `unlockedDate` INTEGER, `isUnlocked` INTEGER NOT NULL, `progressCurrent` INTEGER NOT NULL, `progressTarget` INTEGER NOT NULL, `category` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL)")
                }
            }

            fun getDatabase(context: Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "motivation_database"
                    )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
