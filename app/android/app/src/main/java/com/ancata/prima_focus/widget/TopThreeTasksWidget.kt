package com.ancata.prima_focus.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ancata.prima_focus.MainActivity
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.widget.action.CompleteTaskGlanceAction

class TopThreeTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = PrimaFocusDatabase.getDatabase(context)
        val topTasks = db.taskDao().getTopThreeTasksNow()

        provideContent {
            TopThreeTasksContent(context = context, tasks = topTasks)
        }
    }
}

class TopThreeTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopThreeTasksWidget()
}

@Composable
fun TopThreeTasksContent(context: Context, tasks: List<TaskEntity>) {
    val mainComponent = ComponentName(context, MainActivity::class.java)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1E1E2E)))
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 Top 3 Tareas",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFB86C)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            // Open app shortcut badge
            Box(
                modifier = GlanceModifier
                    .clickable(actionStartActivity(mainComponent))
                    .padding(4.dp)
            ) {
                Text(
                    text = "Abrir ↗",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8BE9FD)),
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(mainComponent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎉 ¡Todo al día!\nSin tareas pendientes",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF50FA7B)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                tasks.forEachIndexed { index, task ->
                    TaskGlanceRow(index = index + 1, task = task, mainComponent = mainComponent)
                    if (index < tasks.size - 1) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskGlanceRow(index: Int, task: TaskEntity, mainComponent: ComponentName) {
    val priorityColor = when {
        task.priorityScore >= 70.0 -> Color(0xFFFF5555) // High priority red
        task.priorityScore >= 40.0 -> Color(0xFFFFB86C) // Medium priority orange
        else -> Color(0xFF50FA7B) // Low priority green
    }

    val categoryText = buildString {
        val catCap = task.category.replaceFirstChar { it.uppercase() }
        val subCatCap = task.subcategory?.replaceFirstChar { it.uppercase() }
        val baseCat = if (catCap.isNotBlank()) catCap else "General"
        append(if (!subCatCap.isNullOrBlank()) "$baseCat • $subCatCap" else baseCat)
        task.date?.let { append(" • $it") }
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color(0xFF282A36)))
            .cornerRadius(10.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority indicator dot / Number
        Box(
            modifier = GlanceModifier
                .size(22.dp)
                .background(ColorProvider(priorityColor))
                .cornerRadius(11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF282A36)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Task Title & Category (clickable to open app)
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(mainComponent))
        ) {
            Text(
                text = task.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFF8F8F2)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = categoryText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF6272A4)),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Complete Button (✓)
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(ColorProvider(Color(0xFF50FA7B)))
                .cornerRadius(14.dp)
                .clickable(
                    actionRunCallback<CompleteTaskGlanceAction>(
                        actionParametersOf(CompleteTaskGlanceAction.TaskIdKey to task.taskId)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF282A36)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
