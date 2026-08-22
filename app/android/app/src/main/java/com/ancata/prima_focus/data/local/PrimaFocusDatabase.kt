package com.ancata.prima_focus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ancata.prima_focus.data.local.dao.SessionDao
import com.ancata.prima_focus.data.local.dao.TaskDao
import com.ancata.prima_focus.data.local.entity.EventEntity
import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        SessionEntity::class,
        EventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PrimaFocusDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: PrimaFocusDatabase? = null

        /** Adds the recurrenceGroupId column introduced in v2. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceGroupId TEXT DEFAULT NULL")
            }
        }

        /**
         * Removes the subtasksCount column (feature dropped in v1.1.1).
         * SQLite does not support DROP COLUMN directly, so the table is recreated.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE tasks_new (
                        taskId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        category TEXT NOT NULL,
                        subcategory TEXT,
                        categoryWeight REAL NOT NULL,
                        date TEXT,
                        time TEXT,
                        hasTime INTEGER NOT NULL DEFAULT 0,
                        timeUrgency REAL NOT NULL DEFAULT 0.0,
                        estimatedMinutes INTEGER,
                        isProject INTEGER NOT NULL DEFAULT 0,
                        recurrence TEXT,
                        recurrenceGroupId TEXT DEFAULT NULL,
                        manualBoost REAL NOT NULL DEFAULT 0.0,
                        nonPostponable INTEGER NOT NULL DEFAULT 0,
                        priorityScore REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'pending',
                        postponedReason TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        meta TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO tasks_new
                    SELECT taskId, title, description, category, subcategory, categoryWeight,
                           date, time, hasTime, timeUrgency, estimatedMinutes,
                           isProject, recurrence, recurrenceGroupId, manualBoost,
                           nonPostponable, priorityScore, status, postponedReason,
                           createdAt, updatedAt, meta
                    FROM tasks
                """.trimIndent())
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        /**
         * Removes the durationMinutes column from sessions table.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF;")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sessions_new (
                        sessionId TEXT NOT NULL PRIMARY KEY,
                        taskId TEXT,
                        startAt INTEGER NOT NULL,
                        endAt INTEGER,
                        mode TEXT,
                        result TEXT,
                        feeling INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE SET_NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO sessions_new (sessionId, taskId, startAt, endAt, mode, result, feeling, createdAt, updatedAt)
                    SELECT sessionId, taskId, startAt, endAt, mode, result, feeling, createdAt, updatedAt
                    FROM sessions
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS sessions")
                db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_taskId ON sessions(taskId)")
                db.execSQL("PRAGMA foreign_keys=ON;")
            }
        }

        fun getDatabase(context: Context): PrimaFocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrimaFocusDatabase::class.java,
                    "primafocus_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

