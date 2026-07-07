package com.ancata.prima_focus.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.service.TimerService
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskId: String,
    taskTitle: String,
    estimatedMinutes: Int,
    onMinimize: () -> Unit,
    onComplete: () -> Unit
) {
    var totalSeconds by remember { mutableIntStateOf(estimatedMinutes * 60) }
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val glows = LocalPremiumGlows.current

    LaunchedEffect(isRunning) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = if (isRunning) "START" else "STOP"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(600L)
            val stopIntent = Intent(context, TimerService::class.java).apply { action = "STOP" }
            context.startService(stopIntent)
            onComplete()
        }
    }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    // Isolate recomposition for timer by keeping state read local to the text/progress
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glows.backgroundCenter, glows.backgroundEdge),
                        center = Offset(size.width / 2f, size.height / 3f),
                        radius = size.height * 0.7f
                    )
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = taskTitle, 
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onMinimize) {
                            Icon(Icons.Default.Close, contentDescription = "Minimize", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ajustar a 15 min") },
                                onClick = {
                                    totalSeconds = 15 * 60
                                    remainingSeconds = totalSeconds
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Abandonar") },
                                onClick = { 
                                    showMenu = false
                                    onMinimize()
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Focus Pill
                Box(
                    modifier = Modifier
                        .background(glows.glassSurface, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Modo Focus",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Circular Counter
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    // Glass background for timer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(glows.glassSurface)
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(glows.glassBorderStart, glows.glassBorderEnd)),
                                CircleShape
                            )
                    )
                    
                    CircularProgressIndicator(
                        progress = { if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f },
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        color = glows.primaryGlow,
                        trackColor = Color.Transparent,
                        strokeWidth = 4.dp
                    )
                    
                    if (showConfetti) {
                        Text("🎉", fontSize = 64.sp)
                    } else {
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 72.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))

                // Big prominent play/pause button
                FloatingActionButton(
                    onClick = { isRunning = !isRunning },
                    shape = CircleShape,
                    containerColor = glows.primaryAccent,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 16.dp),
                    modifier = Modifier.size(96.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isRunning) "Pausar" else "Reanudar",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Done button
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        showConfetti = true
                    },
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = null // No hard border, rely on text
                ) {
                    Text("Terminé", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }
        }
    }
}
