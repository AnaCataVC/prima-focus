package com.ancata.prima_focus.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ancata.prima_focus.MainActivity
import com.ancata.prima_focus.R
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.domain.PriorityEngine
import kotlinx.coroutines.flow.first
import com.ancata.prima_focus.utils.Constants
import com.ancata.prima_focus.utils.TimeUtils

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = PrimaFocusDatabase.getDatabase(context)
            val taskDao = db.taskDao()
            val priorityEngine = PriorityEngine()

            val sharedPrefs = context.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE)
            val dndEnabled = sharedPrefs.getBoolean(Constants.PREF_DISCONNECT_MODE_ENABLED, false)
            if (dndEnabled) {
                val start = sharedPrefs.getString(Constants.PREF_DISCONNECT_START_TIME, "22:00") ?: "22:00"
                val end = sharedPrefs.getString(Constants.PREF_DISCONNECT_END_TIME, "08:00") ?: "08:00"
                
                if (TimeUtils.isCurrentlyInQuietHours(start, end)) {
                    return Result.success()
                }
            }

            val pendingTasks = taskDao.getPendingTasksOrderedByPriority().first()

            if (pendingTasks.isEmpty()) {
                return Result.success()
            }

            val updatedTasks = pendingTasks.map { task ->
                priorityEngine.calculatePriority(task)
            }

            updatedTasks.forEach {
                taskDao.updateTask(it)
            }

            val today = java.time.LocalDate.now()
            val topTask = updatedTasks
                .filter { !TimeUtils.isFutureScheduled(it.date, today) }
                .maxByOrNull { it.priorityScore ?: 0.0 }
            
            topTask?.let {
                val score = it.priorityScore ?: 0.0
                
                createNotificationChannel()
                
                val title = "Prima-Focus: ${it.title}"
                val text = when {
                    score >= 70.0 -> "¡Urgentísimo! Inicia esta tarea ahora."
                    score >= 40.0 -> "Deberías enfocarte en esta tarea pronto."
                    else -> "Para cuando tengas un tiempo libre."
                }
                
                val priority = when {
                    score >= 70.0 -> NotificationCompat.PRIORITY_MAX
                    score >= 40.0 -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(priority)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(Constants.NOTIFICATION_ID, builder.build())
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prima-Focus Tasks"
            val descriptionText = "Notificaciones de la tarea más importante"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
