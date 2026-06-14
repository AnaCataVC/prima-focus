package com.primafocus.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationSchedulerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "TaskNotifications"
    }

    override suspend fun doWork(): Result {
        // In a real implementation, we would read the "TodayTask" from the local database (Room)
        // val task = TaskRepository.getTodayTask()
        // val score = task.priorityScore
        // val isNonPostponable = task.nonPostponable
        
        // Simulated read for handoff
        val score = 75.0 
        val taskTitle = "Enviar informe trimestral"
        val isNonPostponable = false

        createNotificationChannel()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(taskTitle)
            .setPriority(
                when {
                    score >= 70 -> NotificationCompat.PRIORITY_MAX
                    score >= 40 -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }
            )

        when {
            score >= 70 -> {
                builder.setContentText("¡Alta prioridad! Tienes que enfocarte ahora.")
            }
            score >= 40 -> {
                builder.setContentText("Es un buen momento para avanzar en tu Tarea de Hoy.")
            }
            else -> {
                builder.setContentText("Recordatorio suave para tu tarea.")
            }
        }

        val startIntent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_media_play, "Empezar", startIntent)

        if (!isNonPostponable) {
            val snoozeIntent = PendingIntent.getActivity(context, 1, Intent(), PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_popup_sync, "Posponer 1h", snoozeIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios basados en Prioridad"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
