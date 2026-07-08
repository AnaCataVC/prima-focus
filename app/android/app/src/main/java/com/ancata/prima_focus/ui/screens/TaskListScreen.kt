package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState
) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val glows = LocalPremiumGlows.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glows.backgroundEdge)
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
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

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
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingTasks, key = { it.taskId }) { task ->
                        TaskListItem(
                            task = task,
                            onBoost = {
                                viewModel.boostTask(task.taskId)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("¡Prioridad aumentada!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: TaskEntity,
    onBoost: () -> Unit
) {
    val glows = LocalPremiumGlows.current

    val priorityText = when {
        task.priorityScore >= 70 -> "Urgente"
        task.priorityScore >= 40 -> "Alta"
        else -> "Normal"
    }

    val priorityColor = when {
        task.priorityScore >= 70 -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        task.priorityScore >= 40 -> glows.primaryAccent.copy(alpha = 0.8f)
        else -> Color.White.copy(alpha = 0.5f)
    }

    Row(
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val catCap = task.category.replaceFirstChar { it.uppercase() }
            val subCatCap = task.subcategory?.replaceFirstChar { it.uppercase() }
            val catStr = if (!subCatCap.isNullOrBlank()) "$catCap - $subCatCap" else catCap
            val minutesStr = task.estimatedMinutes?.takeIf { it > 0 }?.let { "$it min" } ?: "∞"
            Text(
                text = "$catStr • $minutesStr",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = priorityText,
                style = MaterialTheme.typography.labelSmall,
                color = priorityColor
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        IconButton(
            onClick = onBoost,
            modifier = Modifier
                .size(40.dp)
                .background(glows.primaryAccent.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Boost",
                tint = glows.primaryAccent
            )
        }
    }
}
