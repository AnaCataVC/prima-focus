package com.ancata.prima_focus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ancata.prima_focus.MainActivity
import com.ancata.prima_focus.R
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.prefs.UserPreferences
import com.ancata.prima_focus.utils.Constants
import com.ancata.prima_focus.utils.TimeUtils
import com.ancata.prima_focus.utils.formatCategoryLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class TopTaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /** Redraws every placed instance directly, without a broadcast round-trip. */
        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TopTaskWidgetProvider::class.java))
            if (ids.isNotEmpty()) updateWidgets(context, manager, ids)
        }

        private suspend fun updateWidgets(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
            val pendingTasks = PrimaFocusDatabase.getDatabase(context).taskDao().getPendingTasksListNow()
            val today = LocalDate.now()
            val topTask = pendingTasks.firstOrNull { !TimeUtils.isFutureScheduled(it.date, today) }
            for (appWidgetId in appWidgetIds) {
                manager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, topTask))
            }
        }

        private fun buildViews(context: Context, appWidgetId: Int, topTask: TaskEntity?): RemoteViews {
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
                val emoji = UserPreferences.getInstance(context).categoryEmoji(topTask.category)
                views.setTextViewText(R.id.widget_top_task_title, topTask.title)
                views.setTextViewText(
                    R.id.widget_top_task_category,
                    formatCategoryLine(emoji, topTask.category, topTask.subcategory, topTask.date)
                )
                views.setViewVisibility(R.id.widget_top_task_complete, View.VISIBLE)

                // Complete task action — broadcasts to WidgetActionReceiver
                val completeIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = Constants.ACTION_WIDGET_COMPLETE_TASK
                    putExtra(Constants.EXTRA_TASK_ID, topTask.taskId)
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
            return views
        }
    }
}
