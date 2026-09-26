package com.ancata.prima_focus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CuratedEmojis = listOf(
    "💼", "📊", "💻", "📝", "📅", "📞", "✉️", "📌",
    "💊", "🩺", "🏥", "🧘", "🏃", "🥗", "💪", "😴",
    "🤝", "🎉", "☕", "🍻", "❤️", "💕", "🌹", "💍",
    "🏡", "👨‍👩‍👧", "👶", "🐶", "🌱", "📚", "🎓", "🧠",
    "🧹", "🛒", "🔧", "🧺", "📄", "🏛️", "🪪", "⚖️",
    "💰", "💳", "📈", "🧾", "✈️", "🚗", "🎨", "🎵"
)

/**
 * Curated emoji grid with a free text field as fallback, so any emoji from the system keyboard
 * can be used as well.
 */
@Composable
fun EmojiPickerDialog(
    title: String,
    currentEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customEmoji by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                ) {
                    items(CuratedEmojis) { emoji ->
                        val selected = emoji == currentEmoji
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEmojiSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = if (selected) 30.sp else 24.sp
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = customEmoji,
                    onValueChange = { customEmoji = it.trim().take(MAX_CUSTOM_EMOJI_CHARS) },
                    label = { Text("Otro emoji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEmojiSelected(customEmoji) },
                enabled = customEmoji.isNotBlank()
            ) {
                Text("Usar", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Enough for multi-codepoint emojis (skin tones, ZWJ families) while rejecting whole words.
private const val MAX_CUSTOM_EMOJI_CHARS = 16
