package com.ancata.prima_focus.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.domain.PriorityEngine
import com.ancata.prima_focus.utils.RecurrenceCalculator
import java.time.LocalDate
import java.util.UUID

/**
 * Daily safety-net worker that ensures recurring tasks are never permanently orphaned.
 *
 * This worker runs once per day and scans all completed tasks with a recurrence rule.
 * For each one, it checks whether an active instance of the same series already exists
 * (using recurrenceGroupId). If not, it generates the next occurrence.
 *
 * The on-complete trigger in TaskViewModel handles the common case instantly.
 * This worker covers the edge case where the app was never opened for several days.
 *
 * Idempotency guarantee: hasPendingInGroup() prevents duplicate insertions even if
 * the worker is scheduled multiple times (e.g., after a device reboot).
 */
class RecurrenceReconciliationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = PrimaFocusDatabase.getDatabase(context)
            val taskDao = db.taskDao()
            val priorityEngine = PriorityEngine()
            val now = System.currentTimeMillis()

            val completedRecurringTasks = taskDao.getCompletedRecurringTasks()

            for (task in completedRecurringTasks) {
                val rule = task.recurrence ?: continue
                val groupId = task.recurrenceGroupId ?: task.taskId

                // Skip if an active instance already exists for this series
                if (taskDao.hasPendingInGroup(groupId)) continue

                // Calculate the next occurrence date from the task's scheduled date
                val baseDate = try {
                    if (task.date != null) LocalDate.parse(task.date) else LocalDate.now()
                } catch (e: Exception) {
                    LocalDate.now()
                }

                val nextDate = RecurrenceCalculator.computeNextDate(rule, baseDate) ?: continue

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
                taskDao.insertTask(nextTask)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
