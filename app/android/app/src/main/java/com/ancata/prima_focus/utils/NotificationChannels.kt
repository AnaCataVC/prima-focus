package com.ancata.prima_focus.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Creates the app's single notification channel (no-op when it already exists or below API 26). */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Prima-Focus Tasks",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de la tarea más importante"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
