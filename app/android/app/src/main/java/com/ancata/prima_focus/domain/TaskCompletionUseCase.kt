package com.ancata.prima_focus.domain

import com.ancata.prima_focus.data.local.dao.SessionDao
import com.ancata.prima_focus.data.local.dao.TaskDao
import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.utils.RecurrenceCalculator
import java.time.LocalDate
import java.util.UUID

class TaskCompletionUseCase(
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
    private val priorityEngine: PriorityEngine = PriorityEngine()
) {
    suspend fun execute(
        taskId: String,
        feeling: Int = 3,
        result: String = "direct_complete"
    ): Boolean {
        val now = System.currentTimeMillis()
        val task = taskDao.getTaskById(taskId) ?: return false

        if (task.status == "completed") {
            return false
        }

        val completedTask = task.copy(status = "completed", updatedAt = now, syncVersion = task.syncVersion + 1)

        if (task.recurrence != null) {
            val baseDate = try {
                if (task.date != null) LocalDate.parse(task.date) else LocalDate.now()
            } catch (e: Exception) {
                LocalDate.now()
            }
            val nextDate = RecurrenceCalculator.computeNextDate(task.recurrence, baseDate)

            if (nextDate != null) {
                val groupId = task.recurrenceGroupId ?: task.taskId
                val nextTask = priorityEngine.calculatePriority(
                    task.copy(
                        taskId = UUID.randomUUID().toString(),
                        date = nextDate.toString(),
                        status = "pending",
                        manualBoost = 0.0,
                        postponedReason = null,
                        recurrenceGroupId = groupId,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                taskDao.completeAndSpawnNext(completedTask, nextTask)
            } else {
                taskDao.updateTask(completedTask)
            }
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
}
