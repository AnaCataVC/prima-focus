package com.ancata.prima_focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ancata.prima_focus.ui.viewmodel.CompletedTaskUiModel
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
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    onEditTask: (TaskEntity) -> Unit = {},
    onRequestReview: (String) -> Unit = {}
) {
    val pendingTasks by viewModel.calendarFilteredTasks.collectAsState(initial = emptyList())
    val completedTasks by viewModel.completedTasksList.collectAsState()
    val glows = LocalPremiumGlows.current
    val coroutineScope = rememberCoroutineScope()

    val handleCompleteTask: (TaskEntity) -> Unit = { task ->
        if (viewModel.isHistoryTrackingEnabled.value) {
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

    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Lista de Tareas",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            // Segmented Glassmorphic Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(glows.glassSurface)
                    .border(1.dp, glows.glassBorderStart, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "Pendientes (${pendingTasks.size})",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    text = "Historial (${completedTasks.size})",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            if (selectedTab == 0) {
                // ==================== PENDING TASKS VIEW ====================
                if (pendingTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay tareas pendientes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingTasks, key = { it.taskId }) { task ->
                            TaskListItem(
                                task = task,
                                onComplete = { handleCompleteTask(task) },
                                onBoost = {
                                    viewModel.boostTask(task.taskId)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("¡Prioridad aumentada (+10)!")
                                    }
                                },
                                onDemote = {
                                    viewModel.demoteTask(task.taskId)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Prioridad reducida (-10)")
                                    }
                                },
                                onEdit = { onEditTask(task) },
                                onSnooze = {
                                    viewModel.snoozeTask(task.taskId)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Tarea pospuesta para mañana")
                                    }
                                },
                                onDelete = {
                                    viewModel.deleteTask(task.taskId)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Tarea eliminada",
                                            actionLabel = "Deshacer"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreTask(task)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ==================== COMPLETED TASKS (HISTORY) VIEW ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${completedTasks.size} tareas completadas",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    if (completedTasks.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearHistoryDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vaciar Historial", fontSize = 12.sp)
                        }
                    }
                }

                if (completedTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no tienes tareas en el historial",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(completedTasks, key = { it.taskId }) { completedTask ->
                            CompletedTaskListItem(
                                item = completedTask,
                                onRestore = {
                                    viewModel.uncompleteTask(completedTask.rawTask)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Tarea reabierta y movida a pendientes")
                                    }
                                },
                                onDelete = {
                                    viewModel.deleteCompletedTask(completedTask.taskId)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Registro eliminado del historial")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Vaciar Historial", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "¿Estás seguro de que deseas eliminar todas las tareas completadas y registros de sesión del historial? Esta acción no se puede deshacer.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Historial vaciado correctamente")
                        }
                    }) {
                        Text("Vaciar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = glows.backgroundCenter
            )
        }
    }
}

@Composable
fun TaskListItem(
    task: TaskEntity,
    onComplete: () -> Unit,
    onBoost: () -> Unit,
    onDemote: () -> Unit,
    onEdit: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    val glows = LocalPremiumGlows.current
    var notesExpanded by remember { mutableStateOf(false) }

    val priorityText = when {
        task.priorityScore >= 70 -> "Urgente"
        task.priorityScore >= 40 -> "Alta"
        task.priorityScore < 20 -> "Baja"
        else -> "Normal"
    }

    val priorityColor = when {
        task.priorityScore >= 70 -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        task.priorityScore >= 40 -> glows.primaryAccent.copy(alpha = 0.9f)
        task.priorityScore < 20 -> Color.White.copy(alpha = 0.4f)
        else -> Color.White.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glows.glassSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(glows.glassBorderStart, glows.glassBorderEnd)
                ),
                shape = RoundedCornerShape(16.dp)
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val catCap = task.category.replaceFirstChar { it.uppercase() }
                    val subCatCap = task.subcategory?.replaceFirstChar { it.uppercase() }
                    val catStr = if (!subCatCap.isNullOrBlank()) "$catCap - $subCatCap" else catCap
                    val dateStr = task.date?.let { " • $it" } ?: ""
                    Text(
                        text = "$catStr$dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = priorityText,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (task.manualBoost != 0.0) {
                            val boostLabel = if (task.manualBoost > 0) "+${task.manualBoost.toInt()} boost" else "${task.manualBoost.toInt()} anti-boost"
                            val boostColor = if (task.manualBoost > 0) glows.primaryAccent else MaterialTheme.colorScheme.error
                            Text(
                                text = "($boostLabel)",
                                style = MaterialTheme.typography.labelSmall,
                                color = boostColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Direct Complete Button
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(glows.primaryAccent.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completar tarea",
                        tint = glows.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Notes section if notes exist
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { notesExpanded = !notesExpanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = glows.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (notesExpanded) "Ocultar notas" else "Ver notas",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = if (notesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                AnimatedVisibility(visible = notesExpanded) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp)
                    )
                }
            }

            HorizontalDivider(
                color = glows.glassBorderStart.copy(alpha = 0.3f),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // Action Row: Boost, Anti-Boost, Edit, Snooze, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onBoost,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Boost (Subir prioridad)",
                            tint = glows.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDemote,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Anti-Boost (Bajar prioridad)",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onSnooze,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Posponer a mañana",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val glows = LocalPremiumGlows.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) glows.primaryAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun CompletedTaskListItem(
    item: CompletedTaskUiModel,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val glows = LocalPremiumGlows.current
    var notesExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glows.glassSurface.copy(alpha = 0.5f))
            .border(
                1.dp,
                glows.glassBorderStart.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category & Sentiment Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val catCap = item.category.replaceFirstChar { it.uppercase() }
                    val subCatCap = item.subcategory?.replaceFirstChar { it.uppercase() }
                    val catText = if (!subCatCap.isNullOrBlank()) "$catCap • $subCatCap" else catCap

                    Text(
                        text = catText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "• ${item.feelingEmoji}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Completion Date
                Text(
                    text = item.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = glows.primaryAccent.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Expandable Notes
            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { notesExpanded = !notesExpanded }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = glows.primaryAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (notesExpanded) "Ocultar notas" else "Ver notas",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = if (notesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                }

                AnimatedVisibility(visible = notesExpanded) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
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
                color = glows.glassBorderStart.copy(alpha = 0.2f),
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )

            // Action Row: Reopen / Restore & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.textButtonColors(contentColor = glows.primaryAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reabrir tarea",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reabrir Tarea", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar del historial",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


