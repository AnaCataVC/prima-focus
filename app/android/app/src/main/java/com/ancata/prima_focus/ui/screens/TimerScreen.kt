package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.content.Intent
import android.os.Build
import com.ancata.prima_focus.service.TimerService
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
    // Basic timer state
    var totalSeconds by remember { mutableIntStateOf(estimatedMinutes * 60) }
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            // Simulate 600ms confetti + sound
            delay(600L)
            
            // Stop service
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

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = taskTitle, 
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Default.Close, contentDescription = "Minimize")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
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
                            text = { Text("Silenciar notificaciones") },
                            onClick = { showMenu = false }
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
                    containerColor = MaterialTheme.colorScheme.background
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
            // Circular Counter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    strokeWidth = 12.dp
                )
                if (showConfetti) {
                    Text("🎉", fontSize = 64.sp)
                } else {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(end = 8.dp)
                ) {
                    Text(if (isRunning) "Pausa" else "Reanudar")
                }

                Button(
                    onClick = {
                        isRunning = false
                        showConfetti = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759) // Accent Green
                    )
                ) {
                    Text("Terminé", color = Color.White)
                }
            }
        }
    }
}
