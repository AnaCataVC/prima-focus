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

// Widget surface colors — match existing widget_rounded_bg (white + gray border)
private val WidgetBackground = Color(0xFFFFFFFF)
private val WidgetItemBackground = Color(0xFFF5F5F5)
private val TextPrimary = Color(0xFF000000)
private val TextMuted = Color(0xFF666666)
private val TextCategory = Color(0xFF888888)
private val AccentRose = Color(0xFFF472B6)     // PrimaryRose from Color.kt
private val PriorityHigh = Color(0xFFEC4899)   // RoseAccent
private val PriorityMedium = Color(0xFFF59E0B) // Amber
private val PriorityLow = Color(0xFF4ADE80)    // AccentSage

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
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(16.dp)
            .padding(14.dp)
    ) {
        // Header — same style as widget_top_task_header
        Text(
            text = "Top 3 Tareas",
            style = TextStyle(
                color = ColorProvider(TextMuted),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(mainComponent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todo al día",
                    style = TextStyle(
                        color = ColorProvider(TextMuted),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                tasks.forEachIndexed { index, task ->
                    TaskGlanceRow(task = task, mainComponent = mainComponent)
                    if (index < tasks.size - 1) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskGlanceRow(task: TaskEntity, mainComponent: ComponentName) {
    val priorityColor = when {
        task.priorityScore >= 70.0 -> PriorityHigh
        task.priorityScore >= 40.0 -> PriorityMedium
        else -> PriorityLow
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
            .background(ColorProvider(WidgetItemBackground))
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority dot
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(ColorProvider(priorityColor))
                .cornerRadius(4.dp)
        ) {}

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Task title & category — tapping opens app
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(mainComponent))
        ) {
            Text(
                text = task.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = categoryText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(TextCategory),
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Complete button — rose circle with checkmark
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(ColorProvider(AccentRose))
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
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
