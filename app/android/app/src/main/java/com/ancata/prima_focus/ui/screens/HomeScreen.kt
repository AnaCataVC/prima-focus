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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
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
    onStartTimer: (String, String, Int) -> Unit
) {
    val topTask by viewModel.topTask.collectAsState()
    val glows = LocalPremiumGlows.current
    val coroutineScope = rememberCoroutineScope()

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
            // Top bar
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
                            // No need to snap back, task is deleted and will disappear
                        }
                        else -> {}
                    }
                }

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
                            isBoost -> Icons.Default.KeyboardArrowUp
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
                    // Central Card (Fake Glassmorphism)
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
                    // Subtle corner glow using radial gradient
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
                        // Top Row with Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center, // Centered since boost button is gone
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Priority Badge (Pill)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (task.priorityScore >= 70) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) 
                                                else glows.glassSurface.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Score: ${task.priorityScore.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.87f)
                                )
                            }
                        }

                        // Title
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Meta Row
                        val catStr = if (!task.subcategory.isNullOrBlank()) "${task.category} - ${task.subcategory}" else task.category
                        Text(
                            text = "$catStr • ${task.estimatedMinutes ?: 0} min",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 48.dp)
                        )

                        // Start Button
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
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "EMPEZAR",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp,
                            color = glows.primaryAccent
                        )
                    }
                    } // Closes Central Card Box
                } // End of SwipeToDismissBox
            } else {
                // Empty State
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
