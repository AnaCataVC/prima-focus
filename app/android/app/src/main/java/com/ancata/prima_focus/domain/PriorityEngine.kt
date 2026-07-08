package com.ancata.prima_focus.domain

import com.ancata.prima_focus.data.local.entity.TaskEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.ln

class PriorityEngine {

    /**
     * Calculates and updates a task's priority score.
     * Returns a copy of the entity with the updated score and rules (like isProject) applied.
     */
    fun calculatePriority(task: TaskEntity, currentTimeMs: Long = System.currentTimeMillis()): TaskEntity {
        val ageMs = currentTimeMs - task.createdAt
        val ageDays = (ageMs / (1000.0 * 60 * 60 * 24)).coerceAtLeast(0.0)

        val hasDate = if (task.date != null) 1 else 0

        var timeUrgency = 0.0
        if (task.date != null && task.time != null && task.hasTime) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val scheduledDateTime = LocalDateTime.parse("${task.date} ${task.time}", formatter)
                val currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMs), ZoneId.systemDefault())
                
                val hoursUntil = ChronoUnit.HOURS.between(currentDateTime, scheduledDateTime)
                
                timeUrgency = when {
                    hoursUntil < 0 -> 1.0 
                    hoursUntil <= 2 -> 1.0
                    hoursUntil <= 24 -> 0.6
                    else -> 0.0
                }
            } catch (e: DateTimeParseException) {
                e.printStackTrace()
            }
        }

        val subtasksLn = ln(1.0 + task.subtasksCount)
        val estimatedMin = task.estimatedMinutes ?: 0
        
        var score = (10 * task.categoryWeight) + 
                    (6 * hasDate) + 
                    (8 * timeUrgency) - 
                    (2 * subtasksLn) - 
                    (0.02 * estimatedMin) - 
                    (0.5 * ageDays) + 
                    task.manualBoost

        if (task.categoryWeight >= 4.0 && score < 70.0) {
            score = 70.0
        }

        val isProject = task.isProject || estimatedMin > 180 || task.subtasksCount > 10

        return task.copy(
            priorityScore = score,
            timeUrgency = timeUrgency,
            isProject = isProject,
            updatedAt = currentTimeMs
        )
    }
}
