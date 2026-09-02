package com.ancata.prima_focus.domain

import com.ancata.prima_focus.core.engine.SharedPriorityEngine
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.mapper.toDomain

class PriorityEngine(
    private val sharedEngine: SharedPriorityEngine = SharedPriorityEngine()
) {

    /**
     * Calculates and updates a task's priority score without task duration bias.
     * Returns a copy of the entity with the updated score by delegating to SharedPriorityEngine.
     */
    fun calculatePriority(task: TaskEntity, currentTimeMs: Long = System.currentTimeMillis()): TaskEntity {
        val sharedModel = task.toDomain()
        val calculated = sharedEngine.calculatePriority(sharedModel, currentTimeMs)
        return task.copy(
            priorityScore = calculated.priorityScore,
            timeUrgency = calculated.timeUrgency,
            isProject = calculated.isProject,
            updatedAt = calculated.updatedAt
        )
    }
}

