package com.ancata.prima_focus.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.prefs.UserPreferences
import com.ancata.prima_focus.domain.PriorityEngine
import com.ancata.prima_focus.domain.buildNextOccurrence
import com.ancata.prima_focus.widget.WidgetUpdater

/**
 * Daily safety-net worker that ensures recurring tasks are never permanently orphaned.
 *
 * This worker runs once per day and scans all completed tasks with a recurrence rule.
 * For each one, it checks whether an active instance of the same series already exists
 * (using recurrenceGroupId). If not, it generates the next occurrence.
 *
 * The on-complete trigger in TaskCompletionUseCase handles the common case instantly.
 * This worker covers the edge case where the app was never opened for several days.
 *
 * Idempotency guarantee: hasPendingInGroup() plus deterministic occurrence ids prevent
 * duplicate insertions even if the worker is scheduled multiple times.
 */
class RecurrenceReconciliationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val taskDao = PrimaFocusDatabase.getDatabase(context).taskDao()
            val priorityEngine = PriorityEngine()
            val skipMissedByDefault = UserPreferences.getInstance(context).skipMissedOccurrences
            var spawned = 0

            for (task in taskDao.getCompletedRecurringTasks()) {
                val groupId = task.recurrenceGroupId ?: task.taskId
                if (taskDao.hasPendingInGroup(groupId)) continue

                val nextTask = buildNextOccurrence(task, skipMissedByDefault, priorityEngine) ?: continue
                if (taskDao.getTaskById(nextTask.taskId) != null) continue
                taskDao.insertTask(nextTask)
                spawned++
            }

            if (spawned > 0) WidgetUpdater.refreshAll(context)
            Result.success()
        } catch (e: Exception) {
            Log.e("RecurrenceWorker", "Recurrence reconciliation failed", e)
            Result.retry()
        }
    }
}
