package com.ancata.prima_focus.domain

import com.ancata.prima_focus.data.local.dao.SessionDao
import com.ancata.prima_focus.data.local.dao.TaskDao
import com.ancata.prima_focus.data.local.entity.SessionEntity
import java.time.LocalDate
import java.util.UUID

class TaskCompletionUseCase(
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
    private val priorityEngine: PriorityEngine = PriorityEngine(),
    private val skipMissedByDefault: Boolean = true
) {
    suspend fun execute(
        taskId: String,
        feeling: Int = 3,
        result: String = "direct_complete",
        today: LocalDate = LocalDate.now()
    ): Boolean {
        val now = System.currentTimeMillis()
        val task = taskDao.getTaskById(taskId) ?: return false

        if (task.status == "completed") {
            return false
        }

        val completedTask = task.copy(status = "completed", updatedAt = now, syncVersion = task.syncVersion + 1)
        val nextTask = buildNextOccurrence(task, skipMissedByDefault, priorityEngine, today, now)

        // A deterministic id can already exist (e.g. the occurrence arrived via sync); never overwrite it.
        if (nextTask != null && taskDao.getTaskById(nextTask.taskId) == null) {
            taskDao.completeAndSpawnNext(completedTask, nextTask)
        } else {
            taskDao.updateTask(completedTask)
        }

        val session = SessionEntity(
            sessionId = UUID.randomUUID().toString(),
            taskId = taskId,
            startAt = now,
            endAt = now,
            mode = "direct_complete",
            result = result,
            feeling = feeling,
            createdAt = now,
            updatedAt = now
        )
        sessionDao.insertSession(session)
        return true
    }

    /** Reopens a completed task. Returns false if it does not exist or is not completed. */
    suspend fun uncomplete(taskId: String): Boolean {
        val task = taskDao.getTaskById(taskId) ?: return false
        if (task.status != "completed") return false
        val restored = task.copy(
            status = "pending",
            updatedAt = System.currentTimeMillis(),
            syncVersion = task.syncVersion + 1
        )
        taskDao.updateTask(priorityEngine.calculatePriority(restored))
        return true
    }
}
