package com.ancata.prima_focus.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.ancata.prima_focus.widget.TopThreeTasksWidget
import com.ancata.prima_focus.widget.WidgetActionReceiver

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

        // Optimistic feedback: the tapped row renders as done before the database write finishes.
        updateAppWidgetState(context, glanceId) { it[TopThreeTasksWidget.CompletingTaskKey] = taskId }
        TopThreeTasksWidget().update(context, glanceId)

        updateAppWidgetState(context, glanceId) { it.remove(TopThreeTasksWidget.CompletingTaskKey) }
        WidgetActionReceiver.completeFromWidget(context, taskId, "Completada desde widget Top 3")
    }
}
