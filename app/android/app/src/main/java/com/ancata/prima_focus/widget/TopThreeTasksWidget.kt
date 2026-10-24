package com.ancata.prima_focus.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.text.TextDecoration
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

const val FILTER_ALL = "ALL"
const val FILTER_SINGLE = "SINGLE"
const val FILTER_RECURRING = "RECURRING"

class TopThreeTasksWidget : GlanceAppWidget() {

    // Exact so LocalSize reports the real cell size and the layout can scale to it.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialTasks = loadTopTasks(context, FILTER_ALL)

        provideContent {
            val state = currentState<Preferences>()
            val filterMode = state[FilterModeKey] ?: FILTER_ALL
            // A running Glance session does not re-run provideGlance, so data is reloaded
            // inside the composition whenever refresh() bumps the token or filterMode changes.
            var tasks by remember { mutableStateOf(initialTasks) }
            LaunchedEffect(state[RefreshTokenKey], filterMode) {
                tasks = loadTopTasks(context, filterMode)
            }
            TopThreeTasksContent(
                context = context,
                tasks = tasks,
                filterMode = filterMode,
                completingTaskId = state[CompletingTaskKey]
            )
        }
    }

    companion object {
        /** Task currently being completed from the widget, rendered as done optimistically. */
        val CompletingTaskKey = stringPreferencesKey("completing_task_id")
        val FilterModeKey = stringPreferencesKey("filter_mode")
        private val RefreshTokenKey = longPreferencesKey("refresh_token")

        suspend fun refresh(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(TopThreeTasksWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { it[RefreshTokenKey] = System.nanoTime() }
                TopThreeTasksWidget().update(context, glanceId)
            }
        }

        fun filterTasksByMode(tasks: List<TaskEntity>, filterMode: String): List<TaskEntity> {
            val filtered = when (filterMode) {
                FILTER_SINGLE -> tasks.filter { it.recurrence.isNullOrBlank() }
                FILTER_RECURRING -> tasks.filter { !it.recurrence.isNullOrBlank() }
                else -> tasks
            }
            return filtered.take(3)
        }

        suspend fun loadTopTasks(context: Context, filterMode: String = FILTER_ALL): List<TaskEntity> {
            val today = LocalDate.now()
            val pending = PrimaFocusDatabase.getDatabase(context).taskDao().getPendingTasksListNow()
                .filter { !TimeUtils.isFutureScheduled(it.date, today) }

            return filterTasksByMode(pending, filterMode)
        }
    }
}

class TopThreeTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopThreeTasksWidget()
}

class SetFilterGlanceAction : ActionCallback {
    companion object {
        val TargetFilterKey = ActionParameters.Key<String>("target_filter_key")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val target = parameters[TargetFilterKey] ?: FILTER_ALL
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[TopThreeTasksWidget.FilterModeKey] = target
        }
        TopThreeTasksWidget().update(context, glanceId)
    }
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
fun TopThreeTasksContent(
    context: Context,
    tasks: List<TaskEntity>,
    filterMode: String,
    completingTaskId: String?
) {
    val mainComponent = ComponentName(context, MainActivity::class.java)
    val availableWidth = LocalSize.current.width.value
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
        // Header with filter controls
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top 3 Tareas",
                style = TextStyle(
                    color = ColorProvider(TextMuted),
                    fontSize = metrics.header.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            if (availableWidth >= 280f) {
                // Expanded width: segmented chips
                FilterPill(
                    label = "Todas",
                    isSelected = filterMode == FILTER_ALL,
                    metrics = metrics,
                    onClick = actionRunCallback<SetFilterGlanceAction>(
                        actionParametersOf(SetFilterGlanceAction.TargetFilterKey to FILTER_ALL)
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                FilterPill(
                    label = "Únicas",
                    isSelected = filterMode == FILTER_SINGLE,
                    metrics = metrics,
                    onClick = actionRunCallback<SetFilterGlanceAction>(
                        actionParametersOf(SetFilterGlanceAction.TargetFilterKey to FILTER_SINGLE)
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                FilterPill(
                    label = "Rutinas",
                    isSelected = filterMode == FILTER_RECURRING,
                    metrics = metrics,
                    onClick = actionRunCallback<SetFilterGlanceAction>(
                        actionParametersOf(SetFilterGlanceAction.TargetFilterKey to FILTER_RECURRING)
                    )
                )
            } else {
                // Compact width: cycling filter button
                val currentLabel = when (filterMode) {
                    FILTER_SINGLE -> "Únicas"
                    FILTER_RECURRING -> "Rutinas"
                    else -> "Todas"
                }
                val nextFilter = when (filterMode) {
                    FILTER_ALL -> FILTER_SINGLE
                    FILTER_SINGLE -> FILTER_RECURRING
                    else -> FILTER_ALL
                }

                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(if (filterMode == FILTER_ALL) WidgetItemBackground else AccentRose))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clickable(
                            actionRunCallback<SetFilterGlanceAction>(
                                actionParametersOf(SetFilterGlanceAction.TargetFilterKey to nextFilter)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentLabel ▾",
                        style = TextStyle(
                            color = ColorProvider(if (filterMode == FILTER_ALL) TextMuted else Color.White),
                            fontSize = (metrics.header * 0.85f).coerceIn(10f, 13f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (tasks.isEmpty()) {
            val emptyMessage = when (filterMode) {
                FILTER_SINGLE -> "Sin tareas únicas"
                FILTER_RECURRING -> "Sin rutinas pendientes"
                else -> "Todo al día"
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(mainComponent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
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
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    metrics: RowMetrics,
    onClick: Action
) {
    val bgColor = if (isSelected) AccentRose else WidgetItemBackground
    val textColor = if (isSelected) Color.White else TextMuted
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(bgColor))
            .cornerRadius(10.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = (metrics.header * 0.78f).coerceIn(10f, 13f).sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
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
                        actionParametersOf(
                            CompleteTaskGlanceAction.TaskIdKey to task.taskId,
                            CompleteTaskGlanceAction.SourceWidgetKey to "top_three"
                        )
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
