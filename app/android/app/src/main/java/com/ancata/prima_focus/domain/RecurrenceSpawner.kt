package com.ancata.prima_focus.domain

import com.ancata.prima_focus.core.model.MissedPolicy
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.utils.RecurrenceCalculator
import java.time.LocalDate

/**
 * Builds the next pending occurrence of a recurring task, or null if the task does not recur
 * or its rule cannot produce a next date. Shared by the on-complete path and the daily
 * reconciliation worker so both spawn identical rows (same deterministic id).
 */
fun buildNextOccurrence(
    task: TaskEntity,
    skipMissedByDefault: Boolean,
    priorityEngine: PriorityEngine,
    today: LocalDate = LocalDate.now(),
    now: Long = System.currentTimeMillis()
): TaskEntity? {
    val rule = task.recurrence ?: return null
    val baseDate = task.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    val skipMissed = MissedPolicy.shouldSkip(task.missedPolicy, skipMissedByDefault)
    val nextDate = RecurrenceCalculator.computeNextDate(rule, baseDate, today, skipMissed) ?: return null
    val groupId = task.recurrenceGroupId ?: task.taskId

    return priorityEngine.calculatePriority(
        task.copy(
            taskId = RecurrenceCalculator.instanceId(groupId, nextDate),
            date = nextDate.toString(),
            status = "pending",
            manualBoost = 0.0,
            postponedReason = null,
            recurrenceGroupId = groupId,
            createdAt = now,
            updatedAt = now,
            isDeleted = false,
            deletedAt = null,
            syncVersion = 1L
        )
    )
}
