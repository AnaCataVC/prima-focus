package com.ancata.prima_focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.core.model.PriorityBand
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.ui.components.TaskActionRow
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.utils.formatCategoryLine
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    onStartTimer: (String, String, Int) -> Unit = { _, _, _ -> },
    onEditTask: (TaskEntity) -> Unit = {},
    onRequestReview: (String) -> Unit = {}
) {
    val focusState by viewModel.focusDisplayState.collectAsState()
    val glows = LocalPremiumGlows.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val handleCompleteTask: (TaskEntity) -> Unit = { task ->
        if (viewModel.preferences.isHistoryTrackingEnabled.value) {
            onRequestReview(task.taskId)
        } else {
            viewModel.completeTask(task.taskId, feeling = 3, result = "Quick complete")
            coroutineScope.launch {
                val res = snackbarHostState.showSnackbar(
                    message = "¡Tarea completada!",
                    actionLabel = "Deshacer"
                )
                if (res == SnackbarResult.ActionPerformed) {
                    viewModel.uncompleteTask(task)
                }
            }
        }
    }

    val selectedDate by viewModel.calendarSelectedDate.collectAsState()
    // Collected so a change of emoji in Settings recomposes the cards.
    val categoryEmojis by viewModel.preferences.categoryEmojis.collectAsState()
    val emojiFor: (String) -> String = { category ->
        categoryEmojis[category.lowercase()] ?: viewModel.preferences.categoryEmoji(category)
    }

    val deleteWithUndo: (TaskEntity) -> Unit = { task ->
        viewModel.deleteTask(task.taskId)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar("Tarea eliminada", actionLabel = "Deshacer")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTask(task)
            }
        }
    }
    val snoozeWithFeedback: (TaskEntity) -> Unit = { task ->
        viewModel.snoozeTask(task.taskId)
        coroutineScope.launch { snackbarHostState.showSnackbar("Tarea pospuesta para mañana") }
    }
    val boostWithFeedback: (TaskEntity) -> Unit = { task ->
        viewModel.boostTask(task.taskId)
        coroutineScope.launch { snackbarHostState.showSnackbar("¡Prioridad aumentada!") }
    }
    val demoteWithFeedback: (TaskEntity) -> Unit = { task ->
        viewModel.demoteTask(task.taskId)
        coroutineScope.launch { snackbarHostState.showSnackbar("Prioridad reducida") }
    }

    var tiesExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glows.backgroundCenter, glows.backgroundEdge),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.height * 0.8f
                    )
                )
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerText = if (selectedDate != null) {
                    val isToday = selectedDate == java.time.LocalDate.now()
                    if (isToday) {
                        "HOY (${selectedDate!!.dayOfMonth} ${selectedDate!!.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es-ES")).uppercase()})"
                    } else {
                        "${selectedDate!!.dayOfMonth} ${selectedDate!!.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("es-ES")).uppercase()} ${selectedDate!!.year}"
                    }
                } else {
                    "HOY"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    if (selectedDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✕ Ver todas",
                            style = MaterialTheme.typography.labelSmall,
                            color = glows.primaryAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.setCalendarSelectedDate(null) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sincronizado",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (focusState.heroTask != null) {
                val heroTask = focusState.heroTask!!
                var heroNotesExpanded by remember { mutableStateOf(false) }

                // ==================== HERO FOCUS CARD (#1) ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(glows.glassSurface)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(glows.glassBorderStart, glows.glassBorderEnd)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Priority Badge & Boost indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val band = PriorityBand.fromScore(heroTask.priorityScore)
                            val priorityBg = when (band) {
                                PriorityBand.URGENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                PriorityBand.HIGH -> glows.primaryGlow.copy(alpha = 0.25f)
                                else -> glows.glassSurface.copy(alpha = 0.3f)
                            }
                            val priorityLabel = band.label
                            Box(
                                modifier = Modifier
                                    .background(color = priorityBg, shape = CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = priorityLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                            }
                            if (heroTask.manualBoost != 0.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                val bText = if (heroTask.manualBoost > 0) "+${heroTask.manualBoost.toInt()}" else "${heroTask.manualBoost.toInt()}"
                                Text(
                                    text = "($bText boost)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (heroTask.manualBoost > 0) glows.primaryAccent else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Task Title (Up to 3 lines)
                        Text(
                            text = heroTask.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Category & Date
                        Text(
                            text = formatCategoryLine(emojiFor(heroTask.category), heroTask.category, heroTask.subcategory, heroTask.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Expandable Notes Accordion
                        if (!heroTask.description.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                                    .clickable { heroNotesExpanded = !heroNotesExpanded }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = glows.primaryAccent, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (heroNotesExpanded) "Ocultar notas" else "Ver notas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                                Icon(
                                    imageVector = if (heroNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            AnimatedVisibility(visible = heroNotesExpanded) {
                                Text(
                                    text = heroTask.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Large Complete Checkmark FAB
                        FloatingActionButton(
                            onClick = { handleCompleteTask(heroTask) },
                            shape = CircleShape,
                            containerColor = glows.primaryAccent,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 12.dp,
                                pressedElevation = 16.dp
                            ),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completar Tarea",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "COMPLETAR",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 12.sp,
                            color = glows.primaryAccent
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TaskActionRow(
                            onEdit = { onEditTask(heroTask) },
                            onSnooze = { snoozeWithFeedback(heroTask) },
                            onBoost = { boostWithFeedback(heroTask) },
                            onDemote = { demoteWithFeedback(heroTask) },
                            onDelete = { deleteWithUndo(heroTask) }
                        )
                    }
                }


                // ==================== SECONDARY FOCUS CARDS (#2 & #3) ====================
                if (focusState.secondaryTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Próximas Prioridades",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${focusState.totalPendingCount} pendientes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        focusState.secondaryTasks.forEachIndexed { index, task ->
                            SecondaryTaskCard(
                                rank = index + 2,
                                task = task,
                                emoji = emojiFor(task.category),
                                onComplete = { handleCompleteTask(task) },
                                onEdit = { onEditTask(task) },
                                onSnooze = { snoozeWithFeedback(task) },
                                onBoost = { boostWithFeedback(task) },
                                onDemote = { demoteWithFeedback(task) },
                                onDelete = { deleteWithUndo(task) }
                            )
                        }
                    }
                }

                // ==================== EXPANDABLE TIED TASKS CLUSTER ====================
                if (focusState.tiedTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        onClick = { tiesExpanded = !tiesExpanded },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                        color = glows.glassSurface.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = glows.primaryAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "+${focusState.tiedTasks.size} tareas con igual prioridad",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Icon(
                                imageVector = if (tiesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = tiesExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            focusState.tiedTasks.forEachIndexed { i, tiedTask ->
                                SecondaryTaskCard(
                                    rank = focusState.secondaryTasks.size + 2 + i,
                                    task = tiedTask,
                                    emoji = emojiFor(tiedTask.category),
                                    onComplete = { handleCompleteTask(tiedTask) },
                                    onEdit = { onEditTask(tiedTask) },
                                    onSnooze = { snoozeWithFeedback(tiedTask) },
                                    onBoost = { boostWithFeedback(tiedTask) },
                                    onDemote = { demoteWithFeedback(tiedTask) },
                                    onDelete = { deleteWithUndo(tiedTask) }
                                )
                            }
                        }
                    }
                }

            } else {
                Spacer(modifier = Modifier.height(160.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = glows.primaryAccent.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (selectedDate != null) "Sin tareas para este día" else "¡Todo al día!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedDate != null) "No tienes tareas programadas para la fecha seleccionada" else "Tus prioridades del día aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SecondaryTaskCard(
    rank: Int,
    task: TaskEntity,
    emoji: String,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onSnooze: () -> Unit,
    onBoost: () -> Unit,
    onDemote: () -> Unit,
    onDelete: () -> Unit
) {
    val glows = LocalPremiumGlows.current
    var notesExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(glows.glassSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(glows.glassBorderStart, glows.glassBorderEnd)
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Rank Number Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(glows.primaryAccent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = glows.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCategoryLine(emoji, task.category, task.subcategory, task.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Direct Complete Button
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .size(48.dp)
                        .background(glows.primaryAccent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Notes
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        .clickable { notesExpanded = !notesExpanded }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = glows.primaryAccent, modifier = Modifier.size(12.dp))
                    Text(
                        text = if (notesExpanded) "Ocultar notas" else "Ver notas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = if (notesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }

                AnimatedVisibility(visible = notesExpanded) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp)
                    )
                }
            }

            HorizontalDivider(
                color = glows.glassBorderStart.copy(alpha = 0.25f),
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )

            TaskActionRow(
                onEdit = onEdit,
                onSnooze = onSnooze,
                onBoost = onBoost,
                onDemote = onDemote,
                onDelete = onDelete
            )
        }
    }
}

