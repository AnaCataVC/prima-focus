package com.ancata.prima_focus.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows

/**
 * Secondary task actions with 48dp touch targets: edit and snooze, then the priority pair
 * (boost/demote) after a divider, and delete isolated at the far end to avoid mis-taps.
 */
@Composable
fun TaskActionRow(
    onEdit: () -> Unit,
    onSnooze: () -> Unit,
    onBoost: () -> Unit,
    onDemote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glows = LocalPremiumGlows.current
    val mutedTint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionIcon(Icons.Default.Edit, "Editar", mutedTint, onEdit)
        ActionIcon(Icons.AutoMirrored.Filled.ArrowForward, "Posponer a mañana", mutedTint, onSnooze)
        VerticalDivider(
            modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
            color = glows.glassBorderStart
        )
        ActionIcon(Icons.Default.ArrowUpward, "Subir prioridad", glows.primaryAccent, onBoost)
        ActionIcon(Icons.Default.ArrowDownward, "Bajar prioridad", MaterialTheme.colorScheme.error, onDemote)
        Spacer(modifier = Modifier.weight(1f))
        ActionIcon(Icons.Default.Delete, "Eliminar", MaterialTheme.colorScheme.error.copy(alpha = 0.7f), onDelete)
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}
