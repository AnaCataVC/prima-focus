package com.ancata.prima_focus

import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.widget.FILTER_ALL
import com.ancata.prima_focus.widget.FILTER_RECURRING
import com.ancata.prima_focus.widget.FILTER_SINGLE
import com.ancata.prima_focus.widget.OldestDeferredTaskWidget
import com.ancata.prima_focus.widget.TopThreeTasksWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetLogicTest {

    private fun createTask(
        id: String,
        title: String,
        recurrence: String? = null,
        postponedReason: String? = null,
        date: String? = null,
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L
    ): TaskEntity {
        return TaskEntity(
            taskId = id,
            title = title,
            category = "trabajo",
            categoryWeight = 2.0,
            date = date,
            recurrence = recurrence,
            postponedReason = postponedReason,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    @Test
    fun testFilterTasksByMode_AllReturnsUpToThreeTasks() {
        val tasks = listOf(
            createTask("1", "T1", recurrence = null),
            createTask("2", "T2", recurrence = "DAILY"),
            createTask("3", "T3", recurrence = null),
            createTask("4", "T4", recurrence = "WEEKLY")
        )

        val result = TopThreeTasksWidget.filterTasksByMode(tasks, FILTER_ALL)
        assertEquals(3, result.size)
        assertEquals(listOf("1", "2", "3"), result.map { it.taskId })
    }

    @Test
    fun testFilterTasksByMode_SingleReturnsOnlyNonRecurring() {
        val tasks = listOf(
            createTask("1", "T1", recurrence = null),
            createTask("2", "T2", recurrence = "DAILY"),
            createTask("3", "T3", recurrence = ""),
            createTask("4", "T4", recurrence = "WEEKLY")
        )

        val result = TopThreeTasksWidget.filterTasksByMode(tasks, FILTER_SINGLE)
        assertEquals(2, result.size)
        assertEquals(listOf("1", "3"), result.map { it.taskId })
    }

    @Test
    fun testFilterTasksByMode_RecurringReturnsOnlyRecurring() {
        val tasks = listOf(
            createTask("1", "T1", recurrence = null),
            createTask("2", "T2", recurrence = "DAILY"),
            createTask("3", "T3", recurrence = ""),
            createTask("4", "T4", recurrence = "WEEKLY")
        )

        val result = TopThreeTasksWidget.filterTasksByMode(tasks, FILTER_RECURRING)
        assertEquals(2, result.size)
        assertEquals(listOf("2", "4"), result.map { it.taskId })
    }

    @Test
    fun testSelectOldestDeferredTask_ExplicitlyPostponedTakesPrecedence() {
        val today = LocalDate.of(2026, 10, 24)
        val zone = ZoneId.systemDefault()
        val threeDaysAgoMs = today.minusDays(3).atStartOfDay(zone).toInstant().toEpochMilli()

        val tasks = listOf(
            createTask("1", "Normal Task", createdAt = 500L),
            createTask("2", "Overdue Task", date = "2026-10-20"),
            createTask("3", "Postponed Task", postponedReason = "esperando respuesta", updatedAt = threeDaysAgoMs)
        )

        val info = OldestDeferredTaskWidget.selectOldestDeferredTask(tasks, today)
        assertNotNull(info)
        assertEquals("3", info?.task?.taskId)
        assertEquals("esperando respuesta", info?.reasonText)
        assertEquals("Aplazada hace 3d", info?.badgeText)
    }

    @Test
    fun testSelectOldestDeferredTask_MultiplePostponed_SelectsEarliestUpdatedAt() {
        val today = LocalDate.of(2026, 10, 24)
        val zone = ZoneId.systemDefault()
        val yesterdayMs = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val fiveDaysAgoMs = today.minusDays(5).atStartOfDay(zone).toInstant().toEpochMilli()

        val tasks = listOf(
            createTask("1", "P1", postponedReason = "bloqueado", updatedAt = yesterdayMs),
            createTask("2", "P2", postponedReason = "falta tiempo", updatedAt = fiveDaysAgoMs)
        )

        val info = OldestDeferredTaskWidget.selectOldestDeferredTask(tasks, today)
        assertNotNull(info)
        assertEquals("2", info?.task?.taskId)
        assertEquals("Aplazada hace 5d", info?.badgeText)
    }

    @Test
    fun testSelectOldestDeferredTask_OverdueSelectedWhenNoExplicitPostponed() {
        val today = LocalDate.of(2026, 10, 24)

        val tasks = listOf(
            createTask("1", "Normal Task", date = "2026-10-24"),
            createTask("2", "Overdue 4 days", date = "2026-10-20"),
            createTask("3", "Overdue 2 days", date = "2026-10-22")
        )

        val info = OldestDeferredTaskWidget.selectOldestDeferredTask(tasks, today)
        assertNotNull(info)
        assertEquals("2", info?.task?.taskId)
        assertEquals("Vencida hace 4d", info?.badgeText)
        assertNull(info?.reasonText)
    }

    @Test
    fun testSelectOldestDeferredTask_OldestBacklogSelectedWhenNoOverdueOrPostponed() {
        val today = LocalDate.of(2026, 10, 24)
        val zone = ZoneId.systemDefault()
        val tenDaysAgoMs = today.minusDays(10).atStartOfDay(zone).toInstant().toEpochMilli()
        val twoDaysAgoMs = today.minusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()

        val tasks = listOf(
            createTask("1", "Task Newer", createdAt = twoDaysAgoMs),
            createTask("2", "Task Older", createdAt = tenDaysAgoMs)
        )

        val info = OldestDeferredTaskWidget.selectOldestDeferredTask(tasks, today)
        assertNotNull(info)
        assertEquals("2", info?.task?.taskId)
        assertEquals("En espera hace 10d", info?.badgeText)
    }

    @Test
    fun testSelectOldestDeferredTask_EmptyListReturnsNull() {
        val today = LocalDate.of(2026, 10, 24)
        val info = OldestDeferredTaskWidget.selectOldestDeferredTask(emptyList(), today)
        assertNull(info)
    }
}
