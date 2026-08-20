package com.ancata.prima_focus.domain

import com.ancata.prima_focus.data.local.entity.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class PriorityEngine {

    /**
     * Calculates and updates a task's priority score without task duration bias.
     * Returns a copy of the entity with the updated score.
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
            "El siguiente lunes" -> {
                var nextMonday = currentDate.plusDays(1)
                while (nextMonday.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                    nextMonday = nextMonday.plusDays(1)
                }
                nextMonday
            }
            else -> {
                try {
                    LocalDate.parse(task.date)
                } catch (e: Exception) {
                    null
                }
            }
        }

        var timeUrgency = 0.0
        var scoreDynamic = 0.0

        if (scheduledDate != null) {
            val isOverdue = scheduledDate.isBefore(currentDate)
            val isToday = scheduledDate.isEqual(currentDate)
            val isTomorrow = scheduledDate.isEqual(currentDate.plusDays(1))

            if (task.hasTime && task.time != null) {
                try {
                    val localTime = LocalTime.parse(task.time)
                    val scheduledDateTime = LocalDateTime.of(scheduledDate, localTime)
                    val currentDateTime = LocalDateTime.ofInstant(currentInstant, currentLocalZone)
                    val minutesUntil = ChronoUnit.MINUTES.between(currentDateTime, scheduledDateTime)

                    timeUrgency = when {
                        minutesUntil < 0 -> 1.0
                        minutesUntil <= 120 -> 1.0
                        isToday -> 0.6
                        isTomorrow -> 0.3
                        else -> 0.0
                    }
                } catch (e: Exception) {
                    timeUrgency = when {
                        isOverdue -> 1.0
                        isToday -> 0.6
                        isTomorrow -> 0.3
                        else -> 0.0
                    }
                }
            } else {
                timeUrgency = when {
                    isOverdue -> 1.0
                    isToday -> 0.6
                    isTomorrow -> 0.3
                    else -> 0.0
                }
            }

            scoreDynamic = when {
                isOverdue -> 14.0
                isToday -> (6.0 * hasDate) + (8.0 * timeUrgency)
                isTomorrow -> 4.0
                else -> 2.0
            }
        }

        // Calculate score base with absolute floor at 0.0 to prevent negative decay
        val scoreBaseStatic = ((10.0 * task.categoryWeight) - (0.5 * ageDays)).coerceAtLeast(0.0)

        val scoreBase = if (task.categoryWeight >= 4.0) {
            scoreBaseStatic.coerceAtLeast(70.0)
        } else {
            scoreBaseStatic
        }

        val score = scoreBase + scoreDynamic + task.manualBoost

        return task.copy(
            priorityScore = score,
            timeUrgency = timeUrgency,
            isProject = task.isProject,
            updatedAt = currentTimeMs
        )
    }
}
