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
            try {
                val pendingTasks = dao.getPendingTasksListNow()
                val today = java.time.LocalDate.now()
                val topTask = pendingTasks.firstOrNull { !com.ancata.prima_focus.utils.TimeUtils.isFutureScheduled(it.date, today) }

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_top_task)

                        // Tapping anywhere on the widget opens the app
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val openPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_top_task_container, openPendingIntent)

                        if (topTask != null) {
                            views.setTextViewText(R.id.widget_top_task_title, topTask.title)

                            val catCap = topTask.category.replaceFirstChar { it.uppercase() }
                            val subCatCap = topTask.subcategory?.replaceFirstChar { it.uppercase() }
                            val baseCat = if (catCap.isNotBlank()) catCap else "Sin categoría"
                            val catStr = if (!subCatCap.isNullOrBlank()) "$baseCat - $subCatCap" else baseCat
                            val dateStr = topTask.date?.let { " • $it" } ?: ""

                            views.setTextViewText(R.id.widget_top_task_category, "$catStr$dateStr")
                            views.setViewVisibility(R.id.widget_top_task_complete, View.VISIBLE)

                            // Complete task action — broadcasts to WidgetActionReceiver
                            val completeIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                                action = com.ancata.prima_focus.utils.Constants.ACTION_WIDGET_COMPLETE_TASK
                                putExtra(com.ancata.prima_focus.utils.Constants.EXTRA_TASK_ID, topTask.taskId)
                            }
                            val completePendingIntent = PendingIntent.getBroadcast(
                                context,
                                topTask.taskId.hashCode(),
                                completeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_top_task_complete, completePendingIntent)

                        } else {
                            views.setTextViewText(R.id.widget_top_task_title, "No hay tareas pendientes")
                            views.setTextViewText(R.id.widget_top_task_category, "Todo al día ✓")
                            views.setViewVisibility(R.id.widget_top_task_complete, View.GONE)
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
