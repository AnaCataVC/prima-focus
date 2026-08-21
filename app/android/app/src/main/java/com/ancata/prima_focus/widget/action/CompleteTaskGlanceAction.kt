package com.ancata.prima_focus.widget.action

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.domain.TaskCompletionUseCase
import com.ancata.prima_focus.widget.TopTaskWidgetProvider
import com.ancata.prima_focus.widget.TopThreeTasksWidget

class CompleteTaskGlanceAction : ActionCallback {
    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id_key")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val db = PrimaFocusDatabase.getDatabase(context)
        val useCase = TaskCompletionUseCase(db.taskDao(), db.sessionDao())

        useCase.execute(
            taskId = taskId,
            feeling = 3,
            result = "Completada desde widget Top 3"
        )

        // Refresh all Glance Top 3 widgets
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(TopThreeTasksWidget::class.java)
        glanceIds.forEach { id ->
            TopThreeTasksWidget().update(context, id)
        }

        // Also refresh traditional TopTaskWidget
        val topWidgetIntent = Intent(context, TopTaskWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val topWidgetIds = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, TopTaskWidgetProvider::class.java))
        topWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, topWidgetIds)
        context.sendBroadcast(topWidgetIntent)
    }
}
