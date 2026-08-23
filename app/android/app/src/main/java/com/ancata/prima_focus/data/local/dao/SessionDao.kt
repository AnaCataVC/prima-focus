package com.ancata.prima_focus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ancata.prima_focus.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE taskId = :taskId AND isDeleted = 0")
    fun getSessionsForTask(taskId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    fun getAllSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE isDeleted = 0")
    fun getActiveSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSessions(sessions: List<SessionEntity>)

    @Query("UPDATE sessions SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncVersion = syncVersion + 1 WHERE sessionId = :sessionId")
    fun softDeleteSession(sessionId: String, deletedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sessions SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncVersion = syncVersion + 1 WHERE taskId = :taskId")
    fun softDeleteSessionsForTask(taskId: String, deletedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sessions WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    fun purgeOldTombstones(cutoffTimestamp: Long)

    @Query("DELETE FROM sessions")
    fun clearAllSessions()

    @Query("DELETE FROM sessions WHERE taskId = :taskId")
    fun deleteSessionsForTask(taskId: String)

    @Query("DELETE FROM sessions WHERE taskId NOT IN (SELECT taskId FROM tasks)")
    fun deleteOrphanedSessions()

    /**
     * Atomically merges incoming sync sessions using syncVersion and updatedAt LWW conflict resolution.
     */
    @Transaction
    fun syncMergeSessionsAtomic(receivedSessions: List<SessionEntity>): Int {
        val localSessions = getAllSessions().associateBy { it.sessionId }
        var changesCount = 0
        val sessionsToUpsert = receivedSessions.filter { received ->
            val local = localSessions[received.sessionId]
            if (local == null) {
                true
            } else {
                when {
                    received.syncVersion > local.syncVersion -> true
                    received.syncVersion == local.syncVersion && received.updatedAt > local.updatedAt -> true
                    else -> false
                }
            }
        }
        if (sessionsToUpsert.isNotEmpty()) {
            insertSessions(sessionsToUpsert)
            changesCount = sessionsToUpsert.size
        }
        return changesCount
    }
}
