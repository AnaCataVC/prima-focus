package com.ancata.prima_focus.domain

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.data.local.entity.TaskEntity

class PriorityEngine(
    private val sharedEngine: SharedPriorityEngine = SharedPriorityEngine()
) {

    /**
     * Calculates and updates a task's priority score without task duration bias.
     * Returns a copy of the entity with the updated score by delegating to SharedPriorityEngine.
     */
    fun calculatePriority(task: TaskEntity, currentTimeMs: Long = System.currentTimeMillis()): TaskEntity {
        val sharedModel = task.toCoreModel()
        val calculated = sharedEngine.calculatePriority(sharedModel, currentTimeMs)
        return task.copy(
            priorityScore = calculated.priorityScore,
            timeUrgency = calculated.timeUrgency,
            isProject = calculated.isProject,
            updatedAt = calculated.updatedAt
        )
    }

    private fun TaskEntity.toCoreModel(): Task = Task(
        taskId = taskId,
        title = title,
        description = description,
        category = category,
        subcategory = subcategory,
        categoryWeight = categoryWeight,
        date = date,
        time = time,
        hasTime = hasTime,
        timeUrgency = timeUrgency,
        estimatedMinutes = estimatedMinutes,
        isProject = isProject,
        recurrence = recurrence,
        recurrenceGroupId = recurrenceGroupId,
        manualBoost = manualBoost,
        nonPostponable = nonPostponable,
        priorityScore = priorityScore,
        status = status,
        postponedReason = postponedReason,
        createdAt = createdAt,
        updatedAt = updatedAt,
        meta = meta,
        isDeleted = isDeleted,
        deletedAt = deletedAt,
        syncVersion = syncVersion
    )
}

