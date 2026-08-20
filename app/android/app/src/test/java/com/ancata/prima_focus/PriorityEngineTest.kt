package com.ancata.prima_focus

import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.domain.PriorityEngine
import org.junit.Assert.*
import org.junit.Test

class PriorityEngineTest {

    private val engine = PriorityEngine()

    @Test
    fun calculatePriority_withPositiveBoost_increasesPriorityScore() {
        val now = System.currentTimeMillis()
        val baseTask = TaskEntity(
            taskId = "task-1",
            title = "Test Task",
            description = "Some notes here",
            category = "trabajo",
            subcategory = "entrega",
            categoryWeight = 3.5,
            date = "Hoy",
            time = null,
            estimatedMinutes = 25,
            recurrence = null,
            recurrenceGroupId = null,
            createdAt = now,
            updatedAt = now,
            meta = null,
            postponedReason = null
        )

        val unboosted = engine.calculatePriority(baseTask, now)
        val boosted = engine.calculatePriority(baseTask.copy(manualBoost = 10.0), now)

        assertEquals(10.0, boosted.priorityScore - unboosted.priorityScore, 0.001)
        assertEquals("Some notes here", boosted.description)
    }

    @Test
    fun calculatePriority_withNegativeAntiBoost_decreasesPriorityScore() {
        val now = System.currentTimeMillis()
        val baseTask = TaskEntity(
            taskId = "task-2",
            title = "Deprioritized Task",
            description = "Detailed micro steps",
            category = "casa",
            subcategory = "quehaceres",
            categoryWeight = 2.0,
            date = null,
            time = null,
            estimatedMinutes = 30,
            recurrence = null,
            recurrenceGroupId = null,
            createdAt = now,
            updatedAt = now,
            meta = null,
            postponedReason = null
        )

        val normalTask = engine.calculatePriority(baseTask, now)
        val demotedTask = engine.calculatePriority(baseTask.copy(manualBoost = -10.0), now)

        assertTrue(demotedTask.priorityScore < normalTask.priorityScore)
        assertEquals(-10.0, demotedTask.priorityScore - normalTask.priorityScore, 0.001)
    }

    @Test
    fun calculatePriority_preservesNotesAndProperties() {
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            taskId = "task-3",
            title = "Task with long notes",
            description = "Line 1\nLine 2\nLine 3",
            category = "salud",
            subcategory = "cita médica",
            categoryWeight = 4.0,
            date = "Hoy",
            time = "10:00",
            hasTime = true,
            estimatedMinutes = 45,
            recurrence = null,
            recurrenceGroupId = null,
            createdAt = now,
            updatedAt = now,
            meta = null,
            postponedReason = null
        )

        val processed = engine.calculatePriority(task, now)

        assertEquals("Line 1\nLine 2\nLine 3", processed.description)
        assertTrue(processed.priorityScore >= 70.0) // Health categories have base score clamped at least 70
    }

    @Test
    fun uncompleteTask_recalculatesPriorityDeterministically() {
        val now = System.currentTimeMillis()
        val completedTask = TaskEntity(
            taskId = "task-completed",
            title = "Completed Work",
            description = "Finished draft",
            category = "trabajo",
            subcategory = "entrega",
            categoryWeight = 3.5,
            date = "Hoy",
            time = null,
            estimatedMinutes = 25,
            recurrence = null,
            recurrenceGroupId = null,
            status = "completed",
            createdAt = now - 3600000,
            updatedAt = now,
            meta = null,
            postponedReason = null
        )

        val restoredTask = completedTask.copy(status = "pending", updatedAt = now)
        val prioritized = engine.calculatePriority(restoredTask, now)

        assertEquals("pending", prioritized.status)
        assertTrue(prioritized.priorityScore > 0.0)
        assertEquals("Finished draft", prioritized.description)
    }

    @Test
    fun calculatePriority_withHighAgeDays_clampsBaseScoreAtZero() {
        val now = System.currentTimeMillis()
        val hundredDaysAgo = now - (100L * 24 * 60 * 60 * 1000)
        val oldTask = TaskEntity(
            taskId = "old-task",
            title = "Ancient Task",
            description = null,
            category = "casa",
            subcategory = "quehaceres",
            categoryWeight = 2.0, // static initial: 20.0
            date = null,
            time = null,
            estimatedMinutes = null,
            recurrence = null,
            recurrenceGroupId = null,
            createdAt = hundredDaysAgo,
            updatedAt = hundredDaysAgo,
            meta = null,
            postponedReason = null
        )

        val processed = engine.calculatePriority(oldTask, now)
        // Without floor clamp, 20.0 - 0.5 * 100 = -30.0. With clamp, it must be exactly 0.0
        assertEquals(0.0, processed.priorityScore, 0.001)
    }

    @Test
    fun calculatePriority_todayTaskDominatesTomorrowTask() {
        val now = System.currentTimeMillis()
        val todayTask = TaskEntity(
            taskId = "today-task",
            title = "Task for Today",
            description = null,
            category = "trabajo",
            subcategory = "entrega",
            categoryWeight = 3.5,
            date = "Hoy",
            time = null,
            estimatedMinutes = null,
            recurrence = null,
            recurrenceGroupId = null,
            createdAt = now,
            updatedAt = now,
            meta = null,
            postponedReason = null
        )

        val tomorrowTask = todayTask.copy(
            taskId = "tomorrow-task",
            title = "Task for Tomorrow",
            date = "Mañana"
        )

        val processedToday = engine.calculatePriority(todayTask, now)
        val processedTomorrow = engine.calculatePriority(tomorrowTask, now)

        assertTrue(processedToday.priorityScore > processedTomorrow.priorityScore)
        // Today has 10.8 dynamic score, Tomorrow has 4.0 dynamic score (difference of 6.8)
        assertEquals(6.8, processedToday.priorityScore - processedTomorrow.priorityScore, 0.001)
    }

    @Test
    fun formatEpochToDisplay_formatsTodayCorrectly() {
        val now = System.currentTimeMillis()
        val formatted = com.ancata.prima_focus.utils.TimeUtils.formatEpochToDisplay(now)
        assertTrue(formatted.startsWith("Hoy,"))
    }
}
