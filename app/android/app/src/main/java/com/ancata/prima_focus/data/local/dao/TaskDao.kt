package com.ancata.prima_focus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ancata.prima_focus.core.sync.MergeAction
import com.ancata.prima_focus.core.sync.SyncMergeEngine
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.mapper.toDomain
import com.ancata.prima_focus.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'pending' AND isDeleted = 0
        ORDER BY 
            priorityScore DESC, 
            hasTime DESC, 
            CASE WHEN date IS NULL THEN 1 ELSE 0 END, 
            date ASC, 
            createdAt ASC, 
            taskId ASC
    """)
    fun getPendingTasksOrderedByPriority(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'pending' AND isDeleted = 0
        ORDER BY 
            priorityScore DESC, 
            hasTime DESC, 
            CASE WHEN date IS NULL THEN 1 ELSE 0 END, 
            date ASC, 
            createdAt ASC, 
            taskId ASC
    """)
    suspend fun getPendingTasksListNow(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    fun getActiveTasks(): List<TaskEntity>

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = 'completed' AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getCompletedTasksWithSessions(): Flow<List<com.ancata.prima_focus.data.local.entity.TaskWithSessions>>

    @Query("DELETE FROM tasks WHERE status = 'completed'")
    fun deleteCompletedTasks()

    @Query("SELECT * FROM tasks WHERE status = 'pending' AND isDeleted = 0 ORDER BY priorityScore DESC, hasTime DESC, createdAt ASC LIMIT 1")
    suspend fun getTopTaskNow(): TaskEntity?

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'pending' AND isDeleted = 0
        ORDER BY 
            priorityScore DESC, 
            hasTime DESC, 
            CASE WHEN date IS NULL THEN 1 ELSE 0 END, 
            date ASC, 
            createdAt ASC, 
            taskId ASC 
        LIMIT 3
    """)
    suspend fun getTopThreeTasksNow(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTasks(tasks: List<TaskEntity>)

    @Update
    fun updateTask(task: TaskEntity)

    @Update
    fun updateTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncVersion = syncVersion + 1 WHERE taskId = :taskId")
    fun softDeleteTask(taskId: String, deletedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, syncVersion = syncVersion + 1 WHERE recurrenceGroupId = :groupId")
    fun softDeleteTasksByGroupId(groupId: String, deletedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE recurrenceGroupId = :groupId")
    fun deleteTasksByGroupId(groupId: String)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    fun purgeOldTombstones(cutoffTimestamp: Long)

    @Query("DELETE FROM tasks")
    fun clearAllTasks()

    /**
     * Atomically marks a task as completed and inserts the next occurrence.
     */
    @Transaction
    fun completeAndSpawnNext(completed: TaskEntity, next: TaskEntity) {
        updateTask(completed)
        insertTask(next)
    }

    /**
     * Atomically replaces or restores tasks.
     */
    @Transaction
    fun restoreTasksAtomic(tasks: List<TaskEntity>, clearExisting: Boolean) {
        if (clearExisting) {
            clearAllTasks()
        }
        insertTasks(tasks)
    }

    /**
     * Atomically merges incoming sync tasks using syncVersion and updatedAt LWW conflict resolution.
     */
    @Transaction
    fun syncMergeTasksAtomic(
        receivedTasks: List<TaskEntity>,
        priorityCalculator: (TaskEntity) -> TaskEntity
    ): Int {
        val localTasks = getAllTasks().associateBy { it.taskId }
        val tasksToUpsert = mutableListOf<TaskEntity>()

        receivedTasks.forEach { received ->
            val local = localTasks[received.taskId]
            val localDomain = local?.toDomain()
            val receivedDomain = received.toDomain()

            when (val action = SyncMergeEngine.resolveTaskConflict(localDomain, receivedDomain)) {
                is MergeAction.Insert -> {
                    val entity = action.item.toEntity()
                    val processed = if (entity.status == "pending" && !entity.isDeleted) priorityCalculator(entity) else entity
                    tasksToUpsert.add(processed)
                }
                is MergeAction.Update -> {
                    val entity = action.item.toEntity()
                    val processed = if (entity.status == "pending" && !entity.isDeleted) priorityCalculator(entity) else entity
                    tasksToUpsert.add(processed)
                }
                is MergeAction.KeepLocal -> {}
            }
        }

        if (tasksToUpsert.isNotEmpty()) {
            insertTasks(tasksToUpsert)
        }
        return tasksToUpsert.size
    }

    /**
     * Returns all completed tasks that have a recurrence rule set.
     */
    @Query("SELECT * FROM tasks WHERE status = 'completed' AND isDeleted = 0 AND recurrence IS NOT NULL")
    fun getCompletedRecurringTasks(): List<TaskEntity>

    /**
     * Returns true if there is already a pending or in-progress task belonging
     * to the given recurrence group.
     */
    @Query(
        "SELECT COUNT(*) > 0 FROM tasks " +
        "WHERE recurrenceGroupId = :groupId AND status IN ('pending', 'in_progress') AND isDeleted = 0"
    )
    fun hasPendingInGroup(groupId: String): Boolean
}

