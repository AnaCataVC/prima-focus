package com.ancata.prima_focus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ancata.prima_focus.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'pending' 
        ORDER BY 
            priorityScore DESC, 
            hasTime DESC, 
            CASE WHEN date IS NULL THEN 1 ELSE 0 END, 
            date ASC, 
            createdAt ASC, 
            taskId ASC
    """)
    fun getPendingTasksOrderedByPriority(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): List<TaskEntity>

    @Transaction
    @Query("SELECT * FROM tasks WHERE status = 'completed' ORDER BY updatedAt DESC")
    fun getCompletedTasksWithSessions(): Flow<List<com.ancata.prima_focus.data.local.entity.TaskWithSessions>>

    @Query("DELETE FROM tasks WHERE status = 'completed'")
    fun deleteCompletedTasks()

    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY priorityScore DESC, hasTime DESC, createdAt ASC LIMIT 1")
    suspend fun getTopTaskNow(): TaskEntity?

    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'pending' 
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

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE recurrenceGroupId = :groupId")
    fun deleteTasksByGroupId(groupId: String)

    @Query("DELETE FROM tasks")
    fun clearAllTasks()

    /**
     * Atomically marks a task as completed and inserts the next occurrence.
     * Using @Transaction guarantees both operations succeed or both fail,
     * preventing orphaned tasks if the process dies mid-operation.
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
     * Returns all completed tasks that have a recurrence rule set.
     * Used by RecurrenceReconciliationWorker to find series that may need a new instance.
     */
    @Query("SELECT * FROM tasks WHERE status = 'completed' AND recurrence IS NOT NULL")
    fun getCompletedRecurringTasks(): List<TaskEntity>

    /**
     * Returns true if there is already a pending or in-progress task belonging
     * to the given recurrence group. Used for idempotency in the reconciliation worker.
     */
    @Query(
        "SELECT COUNT(*) > 0 FROM tasks " +
        "WHERE recurrenceGroupId = :groupId AND status IN ('pending', 'in_progress')"
    )
    fun hasPendingInGroup(groupId: String): Boolean
}

