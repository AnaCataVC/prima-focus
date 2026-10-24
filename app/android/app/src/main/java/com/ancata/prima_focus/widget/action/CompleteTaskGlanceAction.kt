package com.ancata.prima_focus.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.ancata.prima_focus.widget.OldestDeferredTaskWidget
import com.ancata.prima_focus.widget.TopThreeTasksWidget
import com.ancata.prima_focus.widget.WidgetActionReceiver

class CompleteTaskGlanceAction : ActionCallback {
    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id_key")
        val SourceWidgetKey = ActionParameters.Key<String>("source_widget_key")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val source = parameters[SourceWidgetKey] ?: "top_three"

        if (source == "oldest_deferred") {
            // Optimistic feedback: the card renders as completing before db write
            updateAppWidgetState(context, glanceId) { it[OldestDeferredTaskWidget.CompletingTaskKey] = taskId }
            OldestDeferredTaskWidget().update(context, glanceId)

            updateAppWidgetState(context, glanceId) { it.remove(OldestDeferredTaskWidget.CompletingTaskKey) }
            WidgetActionReceiver.completeFromWidget(context, taskId, "Completada desde widget Antipostergación")
        } else {
            // Optimistic feedback for TopThreeTasksWidget
            updateAppWidgetState(context, glanceId) { it[TopThreeTasksWidget.CompletingTaskKey] = taskId }
            TopThreeTasksWidget().update(context, glanceId)

            updateAppWidgetState(context, glanceId) { it.remove(TopThreeTasksWidget.CompletingTaskKey) }
            WidgetActionReceiver.completeFromWidget(context, taskId, "Completada desde widget Top 3")
        }
    }
}
