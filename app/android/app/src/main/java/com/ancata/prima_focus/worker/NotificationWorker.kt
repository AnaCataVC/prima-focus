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

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = PrimaFocusDatabase.getDatabase(context)
            val taskDao = db.taskDao()
            val priorityEngine = PriorityEngine()

            val sharedPrefs = context.getSharedPreferences("prima_focus_prefs", Context.MODE_PRIVATE)
            val dndEnabled = sharedPrefs.getBoolean("disconnect_mode_enabled", false)
            if (dndEnabled) {
                val start = sharedPrefs.getString("disconnect_start_time", "22:00") ?: "22:00"
                val end = sharedPrefs.getString("disconnect_end_time", "08:00") ?: "08:00"
                
                val now = java.util.Calendar.getInstance()
                val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(java.util.Calendar.MINUTE)
                val currentTotal = currentHour * 60 + currentMinute
                
                val startParts = start.split(":").map { it.toInt() }
                val startTotal = startParts[0] * 60 + startParts[1]
                
                val endParts = end.split(":").map { it.toInt() }
                val endTotal = endParts[0] * 60 + endParts[1]
                
                val inQuietHours = if (startTotal < endTotal) {
                    currentTotal in startTotal..endTotal
                } else {
                    currentTotal >= startTotal || currentTotal <= endTotal
                }
                
                if (inQuietHours) {
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

            val topTask = updatedTasks.maxByOrNull { it.priorityScore ?: 0.0 }
            
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

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(priority)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, builder.build())
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
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "prima_focus_channel"
        const val NOTIFICATION_ID = 101
    }
}
