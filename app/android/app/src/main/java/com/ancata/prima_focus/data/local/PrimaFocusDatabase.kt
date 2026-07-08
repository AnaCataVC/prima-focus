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
    version = 2,
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

        fun getDatabase(context: Context): PrimaFocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrimaFocusDatabase::class.java,
                    "primafocus_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

