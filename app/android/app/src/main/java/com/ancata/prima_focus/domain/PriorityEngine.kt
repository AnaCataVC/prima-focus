package com.ancata.prima_focus.domain

import com.ancata.prima_focus.data.local.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

        val currentLocalZone = ZoneId.systemDefault()
        val currentInstant = Instant.ofEpochMilli(currentTimeMs)
        val currentDate = currentInstant.atZone(currentLocalZone).toLocalDate()

        val scheduledDate = when (task.date) {
            null -> null
            "Hoy" -> currentDate
            "Mañana" -> currentDate.plusDays(1)
            else -> {
                try {
                    LocalDate.parse(task.date)
                } catch (e: Exception) {
                    null
                }
            }
        }

        var timeUrgency = 0.0
        if (scheduledDate != null) {
            if (task.hasTime && task.time != null) {
                try {
                    val localTime = LocalTime.parse(task.time)
                    val scheduledDateTime = LocalDateTime.of(scheduledDate, localTime)
                    val currentDateTime = LocalDateTime.ofInstant(currentInstant, currentLocalZone)
                    
                    val minutesUntil = ChronoUnit.MINUTES.between(currentDateTime, scheduledDateTime)
                    
                    timeUrgency = when {
                        minutesUntil < 0 -> if (task.recurrence != null) 1.0 else 1.0
                        minutesUntil <= 120 -> 1.0
                        minutesUntil <= 1440 -> 0.6
                        else -> 0.0
                    }
                } catch (e: Exception) {
                    // Fallback to day-level urgency if time parsing fails
                    val currentDateTime = LocalDateTime.ofInstant(currentInstant, currentLocalZone)
                    val startOfScheduledDay = scheduledDate.atStartOfDay()
                    val minutesUntil = ChronoUnit.MINUTES.between(currentDateTime, startOfScheduledDay)
                    timeUrgency = when {
                        // Recurring tasks with a past date get maximum urgency
                        minutesUntil < 0 && task.recurrence != null -> 1.0
                        minutesUntil < 0 -> 0.6
                        minutesUntil <= 1440 -> 0.6
                        else -> 0.0
                    }
                }
            } else {
                // If user didn't define a specific time, it stays at 0.6 all day (no scaling to 1.0)
                val currentDateTime = LocalDateTime.ofInstant(currentInstant, currentLocalZone)
                val startOfScheduledDay = scheduledDate.atStartOfDay()
                val minutesUntil = ChronoUnit.MINUTES.between(currentDateTime, startOfScheduledDay)
                timeUrgency = when {
                    // Recurring tasks with a past date get maximum urgency
                    minutesUntil < 0 && task.recurrence != null -> 1.0
                    minutesUntil < 0 -> 0.6  // Overdue non-recurring: keep at 0.6
                    minutesUntil <= 1440 -> 0.6 // Within 24 hours of start of day
                    else -> 0.0
                }
            }
        }

        val subtasksLn = ln(1.0 + task.subtasksCount)
        val estimatedMin = task.estimatedMinutes ?: 0

        // Calculate score base and dynamic components
        val scoreBaseStatic = (10 * task.categoryWeight) - 
                              (2 * subtasksLn) - 
                              (0.02 * estimatedMin) - 
                              (0.5 * ageDays)

        val scoreBase = if (task.categoryWeight >= 4.0) {
            scoreBaseStatic.coerceAtLeast(70.0)
        } else {
            scoreBaseStatic
        }

        val scoreDynamic = (6 * hasDate) + (8 * timeUrgency)

        val score = scoreBase + scoreDynamic + task.manualBoost

        val isProject = task.isProject || estimatedMin > 120 || task.subtasksCount > 10

        return task.copy(
            priorityScore = score,
            timeUrgency = timeUrgency,
            isProject = isProject,
            updatedAt = currentTimeMs
        )
    }
}
