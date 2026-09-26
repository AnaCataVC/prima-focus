package com.ancata.prima_focus.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ancata.prima_focus.MainActivity
import com.ancata.prima_focus.core.model.PriorityBand
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.prefs.UserPreferences
import com.ancata.prima_focus.utils.TimeUtils
import com.ancata.prima_focus.utils.formatCategoryLine
import com.ancata.prima_focus.widget.action.CompleteTaskGlanceAction
import java.time.LocalDate

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

private val ContentPadding = 12.dp
private val RowGap = 6.dp

class TopThreeTasksWidget : GlanceAppWidget() {

    // Exact so LocalSize reports the real cell size and the layout can scale to it.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialTasks = loadTopTasks(context)

        provideContent {
            val state = currentState<Preferences>()
            // A running Glance session does not re-run provideGlance, so data is reloaded
            // inside the composition whenever refresh() bumps the token.
            var tasks by remember { mutableStateOf(initialTasks) }
            LaunchedEffect(state[RefreshTokenKey]) { tasks = loadTopTasks(context) }
            TopThreeTasksContent(context = context, tasks = tasks, completingTaskId = state[CompletingTaskKey])
        }
    }

    companion object {
        /** Task currently being completed from the widget, rendered as done optimistically. */
        val CompletingTaskKey = stringPreferencesKey("completing_task_id")
        private val RefreshTokenKey = longPreferencesKey("refresh_token")

        suspend fun refresh(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(TopThreeTasksWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { it[RefreshTokenKey] = System.nanoTime() }
                TopThreeTasksWidget().update(context, glanceId)
            }
        }

        private suspend fun loadTopTasks(context: Context): List<TaskEntity> {
            val today = LocalDate.now()
            return PrimaFocusDatabase.getDatabase(context).taskDao().getPendingTasksListNow()
                .filter { !TimeUtils.isFutureScheduled(it.date, today) }
                .take(3)
        }
    }
}

class TopThreeTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopThreeTasksWidget()
}

/**
 * Typography and control sizes derived from the height each task row actually gets,
 * so the widget fills its cell instead of leaving dead space under the last row.
 */
private class RowMetrics(availableHeight: Float, rowCount: Int) {
    private val slot = (availableHeight / rowCount.coerceAtLeast(1)).coerceAtLeast(36f)

    val title = (slot * 0.26f).coerceIn(14f, 26f)
    val category = (title * 0.75f).coerceIn(11f, 18f)
    val header = (title * 0.8f).coerceIn(12f, 17f)
    val button = (slot * 0.52f).coerceIn(28f, 48f)
    val dot = (button * 0.3f).coerceIn(8f, 14f)
}

@Composable
fun TopThreeTasksContent(context: Context, tasks: List<TaskEntity>, completingTaskId: String?) {
    val mainComponent = ComponentName(context, MainActivity::class.java)
    val listHeight = LocalSize.current.height.value -
        (ContentPadding.value * 2) - 26f - (RowGap.value * (tasks.size - 1).coerceAtLeast(0))
    val metrics = RowMetrics(listHeight, tasks.size)
    val preferences = UserPreferences.getInstance(context)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(16.dp)
            .padding(ContentPadding)
    ) {
        // Header — same style as widget_top_task_header
        Text(
            text = "Top 3 Tareas",
            style = TextStyle(
                color = ColorProvider(TextMuted),
                fontSize = metrics.header.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

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
                        fontSize = metrics.title.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            tasks.forEachIndexed { index, task ->
                TaskGlanceRow(
                    task = task,
                    emoji = preferences.categoryEmoji(task.category),
                    isCompleting = task.taskId == completingTaskId,
                    mainComponent = mainComponent,
                    metrics = metrics,
                    modifier = GlanceModifier.defaultWeight()
                )
                if (index < tasks.size - 1) {
                    Spacer(modifier = GlanceModifier.height(RowGap))
                }
            }
        }
    }
}

@Composable
private fun TaskGlanceRow(
    task: TaskEntity,
    emoji: String,
    isCompleting: Boolean,
    mainComponent: ComponentName,
    metrics: RowMetrics,
    modifier: GlanceModifier = GlanceModifier
) {
    val band = PriorityBand.fromScore(task.priorityScore)
    val priorityColor = when (band) {
        PriorityBand.URGENT -> PriorityHigh
        PriorityBand.HIGH -> PriorityMedium
        else -> PriorityLow
    }

    val categoryText = formatCategoryLine(
        emoji, task.category, task.subcategory, task.date, separator = " • ", fallbackCategory = "General"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorProvider(WidgetItemBackground))
            .cornerRadius(10.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority dot
        Box(
            modifier = GlanceModifier
                .size(metrics.dot.dp)
                .background(ColorProvider(priorityColor))
                .cornerRadius((metrics.dot / 2).dp)
        ) {}

        Spacer(modifier = GlanceModifier.width(10.dp))

        // Task title & category — tapping opens app
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(mainComponent))
        ) {
            Text(
                text = if (isCompleting) "✓ ${task.title}" else task.title,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(if (isCompleting) TextMuted else TextPrimary),
                    fontSize = metrics.title.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleting) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            Text(
                text = categoryText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(TextCategory),
                    fontSize = metrics.category.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Complete button — rose circle with checkmark
        Box(
            modifier = GlanceModifier
                .size(metrics.button.dp)
                .background(ColorProvider(AccentRose))
                .cornerRadius((metrics.button / 2).dp)
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
                    fontSize = (metrics.button * 0.5f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
