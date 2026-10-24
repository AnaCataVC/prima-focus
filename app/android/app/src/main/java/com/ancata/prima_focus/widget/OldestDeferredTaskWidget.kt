package com.ancata.prima_focus.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.prefs.UserPreferences
import com.ancata.prima_focus.utils.TimeUtils
import com.ancata.prima_focus.utils.formatCategoryLine
import com.ancata.prima_focus.widget.action.CompleteTaskGlanceAction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Widget surface colors
private val WidgetBackground = Color(0xFFFFFFFF)
private val WidgetItemBackground = Color(0xFFF5F5F5)
private val TextPrimary = Color(0xFF000000)
private val TextMuted = Color(0xFF666666)
private val TextCategory = Color(0xFF888888)
private val BadgeBackground = Color(0xFFFCE7F3)
private val BadgeTextColor = Color(0xFFBE185D)
private val AccentRose = Color(0xFFF472B6)

private val ContentPadding = 12.dp

data class DeferredTaskInfo(
    val task: TaskEntity,
    val badgeText: String,
    val reasonText: String?
)

class OldestDeferredTaskWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initialInfo = loadOldestDeferredTask(context)

        provideContent {
            val state = currentState<Preferences>()
            var info by remember { mutableStateOf(initialInfo) }

            LaunchedEffect(state[RefreshTokenKey]) {
                info = loadOldestDeferredTask(context)
            }

            OldestDeferredTaskContent(
                context = context,
                info = info,
                completingTaskId = state[CompletingTaskKey]
            )
        }
    }

    companion object {
        val CompletingTaskKey = stringPreferencesKey("completing_task_id")
        private val RefreshTokenKey = longPreferencesKey("refresh_token")

        suspend fun refresh(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(OldestDeferredTaskWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { it[RefreshTokenKey] = System.nanoTime() }
                OldestDeferredTaskWidget().update(context, glanceId)
            }
        }

        fun selectOldestDeferredTask(
            pendingTasks: List<TaskEntity>,
            today: LocalDate = LocalDate.now()
        ): DeferredTaskInfo? {
            if (pendingTasks.isEmpty()) return null

            // Tier 1: Explicitly postponed tasks (postponedReason is set)
            val explicitlyPostponed = pendingTasks.filter { !it.postponedReason.isNullOrBlank() }
            if (explicitlyPostponed.isNotEmpty()) {
                val chosen = explicitlyPostponed.minByOrNull { it.updatedAt } ?: explicitlyPostponed.first()
                val postDate = Instant.ofEpochMilli(chosen.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                val days = ChronoUnit.DAYS.between(postDate, today).coerceAtLeast(0)
                val badge = when (days) {
                    0L -> "Aplazada hoy"
                    1L -> "Aplazada ayer"
                    else -> "Aplazada hace ${days}d"
                }
                return DeferredTaskInfo(chosen, badge, chosen.postponedReason)
            }

            // Tier 2: Overdue tasks (scheduled date in the past)
            val overdue = pendingTasks.mapNotNull { task ->
                val taskDate = task.date?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }
                if (taskDate != null && taskDate.isBefore(today)) {
                    task to taskDate
                } else null
            }
            if (overdue.isNotEmpty()) {
                val (chosen, taskDate) = overdue.minByOrNull { it.second } ?: overdue.first()
                val days = ChronoUnit.DAYS.between(taskDate, today).coerceAtLeast(0)
                val badge = when (days) {
                    1L -> "Vencida ayer"
                    else -> "Vencida hace ${days}d"
                }
                return DeferredTaskInfo(chosen, badge, null)
            }

            // Tier 3: Oldest backlog task by creation timestamp
            val oldest = pendingTasks.minByOrNull { it.createdAt } ?: pendingTasks.first()
            val createdDate = Instant.ofEpochMilli(oldest.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            val days = ChronoUnit.DAYS.between(createdDate, today).coerceAtLeast(0)
            val badge = when (days) {
                0L -> "Creada hoy"
                1L -> "En espera desde ayer"
                else -> "En espera hace ${days}d"
            }
            return DeferredTaskInfo(oldest, badge, null)
        }

        suspend fun loadOldestDeferredTask(context: Context): DeferredTaskInfo? {
            val today = LocalDate.now()
            val pending = PrimaFocusDatabase.getDatabase(context).taskDao().getPendingTasksListNow()
                .filter { !TimeUtils.isFutureScheduled(it.date, today) }

            return selectOldestDeferredTask(pending, today)
        }
    }
}

class OldestDeferredTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OldestDeferredTaskWidget()
}

class StartFocusGlanceAction : ActionCallback {
    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id_key")
        val TitleKey = ActionParameters.Key<String>("title_key")
        val MinutesKey = ActionParameters.Key<Int>("minutes_key")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val title = parameters[TitleKey] ?: ""
        val minutes = parameters[MinutesKey] ?: 25

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.ancata.prima_focus.ACTION_START_TIMER"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("taskId", taskId)
            putExtra("title", title)
            putExtra("minutes", minutes)
        }
        context.startActivity(intent)
    }
}

@Composable
fun OldestDeferredTaskContent(
    context: Context,
    info: DeferredTaskInfo?,
    completingTaskId: String?
) {
    val mainComponent = ComponentName(context, MainActivity::class.java)
    val preferences = UserPreferences.getInstance(context)
    val cellHeight = LocalSize.current.height.value
    val isCompact = cellHeight < 130f

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(16.dp)
            .padding(ContentPadding)
    ) {
        if (info == null) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(mainComponent)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sin tareas aplazadas ✓",
                        style = TextStyle(
                            color = ColorProvider(TextPrimary),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Todo al día",
                        style = TextStyle(
                            color = ColorProvider(TextMuted),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        } else {
            val task = info.task
            val isCompleting = task.taskId == completingTaskId
            val emoji = preferences.categoryEmoji(task.category)
            val categoryLine = formatCategoryLine(
                emoji, task.category, task.subcategory, task.date, separator = " • ", fallbackCategory = "General"
            )

            // Header row: label + time badge
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Más Aplazada",
                    style = TextStyle(
                        color = ColorProvider(TextMuted),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(BadgeBackground))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = info.badgeText,
                        style = TextStyle(
                            color = ColorProvider(BadgeTextColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Middle card: task details
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ColorProvider(WidgetItemBackground))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(mainComponent))
                ) {
                    Text(
                        text = if (isCompleting) "✓ ${task.title}" else task.title,
                        maxLines = if (isCompact) 1 else 2,
                        style = TextStyle(
                            color = ColorProvider(if (isCompleting) TextMuted else TextPrimary),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isCompleting) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = categoryLine,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(TextCategory),
                            fontSize = 11.sp
                        )
                    )
                    if (!info.reasonText.isNullOrBlank() && !isCompact) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "Motivo: ${info.reasonText}",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(BadgeTextColor),
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Actions: "▶ Enfocar" and "✓ Completar"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Start Focus timer button
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(ColorProvider(WidgetItemBackground))
                            .cornerRadius(16.dp)
                            .clickable(
                                actionRunCallback<StartFocusGlanceAction>(
                                    actionParametersOf(
                                        StartFocusGlanceAction.TaskIdKey to task.taskId,
                                        StartFocusGlanceAction.TitleKey to task.title,
                                        StartFocusGlanceAction.MinutesKey to (task.estimatedMinutes ?: 25)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            style = TextStyle(
                                color = ColorProvider(AccentRose),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Complete task button
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(ColorProvider(AccentRose))
                            .cornerRadius(16.dp)
                            .clickable(
                                actionRunCallback<CompleteTaskGlanceAction>(
                                    actionParametersOf(
                                        CompleteTaskGlanceAction.TaskIdKey to task.taskId,
                                        CompleteTaskGlanceAction.SourceWidgetKey to "oldest_deferred"
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
