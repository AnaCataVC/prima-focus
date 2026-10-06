package com.ancata.prima_focus.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ancata.prima_focus.R
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.prefs.UserPreferences
import com.ancata.prima_focus.domain.TaskCompletionUseCase
import com.ancata.prima_focus.utils.Constants
import com.ancata.prima_focus.utils.ensureNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(Constants.EXTRA_TASK_ID) ?: return
        val action = intent.action
        if (action != Constants.ACTION_WIDGET_COMPLETE_TASK && action != Constants.ACTION_UNDO_COMPLETE_TASK) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == Constants.ACTION_WIDGET_COMPLETE_TASK) {
                    completeFromWidget(context, taskId, "Completada desde widget")
                } else {
                    undoCompletion(context, taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WidgetActionReceiver"

        /** Completes a task from any widget, offers a short-lived undo, and redraws the widgets. */
        suspend fun completeFromWidget(context: Context, taskId: String, resultLabel: String) {
            try {
                val db = PrimaFocusDatabase.getDatabase(context)
                val useCase = TaskCompletionUseCase(
                    db.taskDao(),
                    db.sessionDao(),
                    skipMissedByDefault = UserPreferences.getInstance(context).skipMissedOccurrences
                )
                val title = db.taskDao().getTaskById(taskId)?.title
                if (useCase.execute(taskId = taskId, feeling = 3, result = resultLabel) && title != null) {
                    postUndoNotification(context, taskId, title)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete task $taskId from widget", e)
            }
            WidgetUpdater.refreshAll(context)
        }

        private suspend fun undoCompletion(context: Context, taskId: String) {
            NotificationManagerCompat.from(context).cancel(Constants.UNDO_NOTIFICATION_ID)
            try {
                val db = PrimaFocusDatabase.getDatabase(context)
                TaskCompletionUseCase(db.taskDao(), db.sessionDao()).uncomplete(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to undo completion of task $taskId", e)
            }
            WidgetUpdater.refreshAll(context)
        }

        /** Widgets cannot show a snackbar, so undo is offered through a notification that expires quickly. */
        private fun postUndoNotification(context: Context, taskId: String, title: String) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return
            ensureNotificationChannel(context)

            val undoIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = Constants.ACTION_UNDO_COMPLETE_TASK
                putExtra(Constants.EXTRA_TASK_ID, taskId)
            }
            val undoPendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                undoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_task_completed)
                .setContentTitle("✓ $title")
                .setContentText("Tarea completada")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setTimeoutAfter(Constants.UNDO_NOTIFICATION_TIMEOUT_MS)
                .addAction(0, "Deshacer", undoPendingIntent)
                .build()
            try {
                notificationManager.notify(Constants.UNDO_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission revoked; undo not offered", e)
            }
        }
    }
}
