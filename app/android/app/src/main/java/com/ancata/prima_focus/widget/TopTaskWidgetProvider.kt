package com.ancata.prima_focus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ancata.prima_focus.MainActivity
import com.ancata.prima_focus.R
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TopTaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val dao = PrimaFocusDatabase.getDatabase(context).taskDao()

        CoroutineScope(Dispatchers.IO).launch {
            val topTask = dao.getTopTaskNow()

            withContext(Dispatchers.Main) {
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_top_task)

                    if (topTask != null) {
                        views.setTextViewText(R.id.widget_top_task_title, topTask.title)
                        val baseCat = if (topTask.category.isNotBlank()) topTask.category else "Sin categoría"
                        val catStr = if (!topTask.subcategory.isNullOrBlank()) "$baseCat - ${topTask.subcategory}" else baseCat
                        views.setTextViewText(R.id.widget_top_task_category, "$catStr • ${topTask.estimatedMinutes} min")
                        views.setViewVisibility(R.id.widget_top_task_play, View.VISIBLE)

                        val intent = Intent(context, MainActivity::class.java).apply {
                            action = "com.ancata.prima_focus.ACTION_START_TIMER"
                            putExtra("taskId", topTask.taskId)
                            putExtra("title", topTask.title)
                            putExtra("minutes", topTask.estimatedMinutes)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId, // unique per widget instance
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_top_task_play, pendingIntent)
                        
                        // Also clicking the whole widget can just open the app
                        val openAppIntent = Intent(context, MainActivity::class.java)
                        val openAppPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId + 1000,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_top_task_container, openAppPendingIntent)

                    } else {
                        views.setTextViewText(R.id.widget_top_task_title, "No hay tareas pendientes")
                        views.setTextViewText(R.id.widget_top_task_category, "Todo al día")
                        views.setViewVisibility(R.id.widget_top_task_play, View.GONE)
                        
                        val openAppIntent = Intent(context, MainActivity::class.java)
                        val openAppPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_top_task_container, openAppPendingIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                pendingResult.finish()
            }
        }
    }
}
