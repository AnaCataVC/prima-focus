package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.key
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    onStartTimer: (String, String, Int) -> Unit,
    onEditTask: (com.ancata.prima_focus.data.local.entity.TaskEntity) -> Unit = {}
) {
    val topTask by viewModel.topTask.collectAsState()
    val glows = LocalPremiumGlows.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOY",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sincronizado",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (topTask != null) {
                val task = topTask!!
                
                val haptic = LocalHapticFeedback.current
                
                val dismissState = rememberSwipeToDismissBoxState()
                
                LaunchedEffect(dismissState.currentValue) {
                    when(dismissState.currentValue) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.boostTask(task.taskId)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Boost aplicado!")
                            }
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                viewModel.deleteTask(task.taskId)
                                val result = snackbarHostState.showSnackbar(
                                    message = "Tarea eliminada",
                                    actionLabel = "Deshacer"
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreTask(task)
                                    Toast.makeText(context, "Tarea restaurada", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else -> {}
                    }
                }

                key(task.taskId) {
                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                    backgroundContent = {
                        val isBoost = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                        val isDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                        
                        val brush = when {
                            isBoost -> Brush.horizontalGradient(
                                colors = listOf(glows.primaryAccent.copy(alpha = 0.8f), Color.Transparent)
                            )
                            isDelete -> Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Red.copy(alpha = 0.8f))
                            )
                            else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                        
                        val icon = when {
                            isBoost -> Icons.Default.ArrowUpward
                            isDelete -> Icons.Default.Delete
                            else -> null
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(brush),
                            contentAlignment = when {
                                isBoost -> Alignment.CenterStart
                                isDelete -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(horizontal = 32.dp).size(40.dp)
                                )
                            }
                        }
                    }
                ) {
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
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(glows.primaryGlow.copy(alpha = 0.15f), Color.Transparent),
                                        center = Offset(0f, 0f),
                                        radius = size.width / 2f
                                    ),
                                    center = Offset(0f, 0f)
                                )
                            }
                    )

                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (task.isProject) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE5A910).copy(alpha = 0.2f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE5A910), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tarea muy larga — divídela en varias partes", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE5A910))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center, 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (task.priorityScore >= 70) Color(0xFFE65100).copy(alpha = 0.2f) 
                                                else glows.glassSurface.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                val priorityText = when {
                                    task.priorityScore >= 70 -> "Urgente"
                                    task.priorityScore >= 40 -> "Alta"
                                    else -> "Normal"
                                }
                                Text(
                                    text = priorityText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.87f)
                                )
                            }
                        }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val catCap = task.category.replaceFirstChar { it.uppercase() }
                        val subCatCap = task.subcategory?.replaceFirstChar { it.uppercase() }
                        val catStr = if (!subCatCap.isNullOrBlank()) "$catCap - $subCatCap" else catCap
                        val minutesStr = task.estimatedMinutes?.takeIf { it > 0 }?.let { "$it min" } ?: "∞"
                        Text(
                            text = "$catStr • $minutesStr",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 48.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingActionButton(
                                onClick = { 
                                    onStartTimer(task.taskId, task.title, task.estimatedMinutes ?: 25)
                                },
                                shape = CircleShape,
                                containerColor = glows.primaryAccent,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 12.dp,
                                    pressedElevation = 16.dp
                                ),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Empezar",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(32.dp))
                            
                            FloatingActionButton(
                                onClick = { 
                                    viewModel.completeTask(task.taskId, feeling = 3, result = "Quick complete")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("¡Tarea completada!")
                                    }
                                },
                                shape = CircleShape,
                                containerColor = glows.glassSurface.copy(alpha = 0.5f),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completar Rápido",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "EMPEZAR",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp,
                            color = glows.primaryAccent
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onEditTask(task) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White.copy(alpha = 0.5f))
                            }
                            IconButton(onClick = {
                                viewModel.snoozeTask(task.taskId)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Tarea pospuesta para mañana")
                                }
                            }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Posponer", tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    }
                    }
                }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Tu Tarea Hoy aparecerá aquí",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
