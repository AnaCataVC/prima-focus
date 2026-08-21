package com.ancata.prima_focus.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.domain.TaskCompletionUseCase
import com.ancata.prima_focus.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_WIDGET_COMPLETE_TASK) {
            val taskId = intent.getStringExtra(Constants.EXTRA_TASK_ID) ?: return
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = PrimaFocusDatabase.getDatabase(context)
                    val useCase = TaskCompletionUseCase(db.taskDao(), db.sessionDao())
                    useCase.execute(
                        taskId = taskId,
                        feeling = 3,
                        result = "Completada desde widget"
                    )

                    // Trigger refresh of TopTaskWidget
                    val topWidgetIntent = Intent(context, TopTaskWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    }
                    val topWidgetIds = AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, TopTaskWidgetProvider::class.java))
                    topWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, topWidgetIds)
                    context.sendBroadcast(topWidgetIntent)

                    // Trigger refresh of TopThreeTasksWidget (Glance)
                    try {
                        val manager = GlanceAppWidgetManager(context)
                        val glanceIds = manager.getGlanceIds(TopThreeTasksWidget::class.java)
                        glanceIds.forEach { id ->
                            TopThreeTasksWidget().update(context, id)
                        }
                    } catch (e: Throwable) {
                        // In case Glance widget is not placed
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
