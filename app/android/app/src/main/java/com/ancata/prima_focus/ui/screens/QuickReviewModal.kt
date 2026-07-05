package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReviewModal(
    viewModel: TaskViewModel,
    taskId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var completedState by remember { mutableStateOf<String?>(null) } // "yes", "partial", "no"
    var moodState by remember { mutableStateOf<String?>(null) } // "bad", "ok", "good"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Question 1
            Text(
                text = "¿Completaste la tarea?",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusButton(
                    text = "Sí",
                    selected = completedState == "yes",
                    selectedColor = Color(0xFF34C759),
                    onClick = { completedState = "yes" }
                )
                StatusButton(
                    text = "Parcial",
                    selected = completedState == "partial",
                    selectedColor = Color(0xFFFF9F0A),
                    onClick = { completedState = "partial" }
                )
                StatusButton(
                    text = "No",
                    selected = completedState == "no",
                    selectedColor = Color.Gray,
                    onClick = { completedState = "no" }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question 2
            Text(
                text = "¿Cómo te sentiste?",
                style = MaterialTheme.typography.bodyLarge,
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

            // Actions for Partial / No
            if (completedState == "partial" || completedState == "no") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "¿Quieres posponer o dividir en subtareas?",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { /* TODO */ }) {
                                Text("Posponer")
                            }
                            OutlinedButton(onClick = { /* TODO */ }) {
                                Text("Dividir")
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (completedState == "yes") {
                        // TODO: Update task status in viewmodel
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = completedState != null && moodState != null
            ) {
                Text("Guardar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Se guardará en Historial",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
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
    val backgroundColor = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(100.dp)
            .height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmojiButton(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 28.sp)
        }
    }
}
