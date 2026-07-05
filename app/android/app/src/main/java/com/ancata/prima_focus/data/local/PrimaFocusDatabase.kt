package com.ancata.prima_focus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class PrimaFocusDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: PrimaFocusDatabase? = null

        fun getDatabase(context: Context): PrimaFocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrimaFocusDatabase::class.java,
                    "primafocus_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
