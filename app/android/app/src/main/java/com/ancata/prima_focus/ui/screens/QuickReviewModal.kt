package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReviewModal(
    viewModel: TaskViewModel,
    taskId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glows = LocalPremiumGlows.current
    val context = LocalContext.current
    
    var completedState by remember { mutableStateOf<String?>(null) }
    var moodState by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glows.backgroundCenter.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Revisión Rápida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "¿Completaste la tarea?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusButton(
                    text = "Sí",
                    selected = completedState == "yes",
                    selectedColor = Color(0xFF34C759), // Accent Green
                    onClick = { completedState = "yes" }
                )
                StatusButton(
                    text = "Parcial",
                    selected = completedState == "partial",
                    selectedColor = Color(0xFFFF9F0A), // Orange
                    onClick = { completedState = "partial" }
                )
                StatusButton(
                    text = "No",
                    selected = completedState == "no",
                    selectedColor = Color(0xFF8E8E93), // Gray
                    onClick = { completedState = "no" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "¿Cómo te sentiste?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EmojiButton(
                    emoji = "😐",
                    selected = moodState == "bad",
                    onClick = { moodState = "bad" }
                )
                EmojiButton(
                    emoji = "🙂",
                    selected = moodState == "ok",
                    onClick = { moodState = "ok" }
                )
                EmojiButton(
                    emoji = "😃",
                    selected = moodState == "good",
                    onClick = { moodState = "good" }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (completedState == "partial" || completedState == "no") {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(glows.glassSurface)
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(glows.glassBorderStart, glows.glassBorderEnd)),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "¿Quieres posponer esta tarea?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val postponed = viewModel.postponeTask(taskId, reason = completedState ?: "postponed")
                                    if (!postponed) {
                                        Toast.makeText(context, "Esta tarea no puede posponerse", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = null
                            ) {
                                Text("Posponer")
                            }
                            OutlinedButton(
                                onClick = { 
                                    viewModel.splitTask(taskId)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = null
                            ) {
                                Text("Dividir")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val feelingInt = when (moodState) {
                        "bad" -> 1
                        "ok" -> 3
                        "good" -> 5
                        else -> 3
                    }
                    if (completedState == "yes") {
                        viewModel.completeTask(taskId, feeling = feelingInt, result = "completed")
                    } else {
                        viewModel.postponeTask(taskId, reason = completedState ?: "postponed")
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = completedState != null && moodState != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = glows.primaryAccent,
                    disabledContainerColor = glows.primaryAccent.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Guardar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                text = "Se guardará en Historial",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun StatusButton(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val glows = LocalPremiumGlows.current
    val backgroundColor = if (selected) selectedColor else glows.glassSurface
    val textColor = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
    val borderColor = if (selected) Color.Transparent else glows.glassBorderStart

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(100.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmojiButton(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val glows = LocalPremiumGlows.current
    val backgroundColor = if (selected) glows.primaryGlow.copy(alpha = 0.3f) else Color.Transparent
    val borderColor = if (selected) glows.primaryAccent.copy(alpha = 0.5f) else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Text(text = emoji, fontSize = 28.sp)
    }
}
