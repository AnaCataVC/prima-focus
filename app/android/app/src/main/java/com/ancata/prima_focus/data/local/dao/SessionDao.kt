package com.ancata.prima_focus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ancata.prima_focus.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE taskId = :taskId")
    fun getSessionsForTask(taskId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSessions(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions")
    fun clearAllSessions()

    @Query("DELETE FROM sessions WHERE taskId = :taskId")
    fun deleteSessionsForTask(taskId: String)

    @Query("DELETE FROM sessions WHERE taskId NOT IN (SELECT taskId FROM tasks)")
    fun deleteOrphanedSessions()
}
