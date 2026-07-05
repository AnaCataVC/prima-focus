package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val scrollState = rememberScrollState()

    var manualBoost by remember { mutableFloatStateOf(5f) }
    var autoSplit by remember { mutableStateOf(false) }
    var nonPostponableHealth by remember { mutableStateOf(true) }
    var nonPostponableUrgent by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Recurrence section placeholder
        SectionTitle("Recurrencia por defecto")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = false, onClick = { /*TODO*/ }, label = { Text("Diaria") })
            FilterChip(selected = false, onClick = { /*TODO*/ }, label = { Text("Semanal") })
            FilterChip(selected = false, onClick = { /*TODO*/ }, label = { Text("Mensual") })
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Priority Settings
        SectionTitle("Motor de Prioridades")
        
        // Auto-split
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dividir automáticamente", fontWeight = FontWeight.SemiBold)
                Text("Sugerir dividir tareas largas (>120m)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            Switch(checked = autoSplit, onCheckedChange = { autoSplit = it })
        }
        
        // Manual Boost
        Spacer(modifier = Modifier.height(16.dp))
        Text("Manual Boost: ${manualBoost.toInt()}", fontWeight = FontWeight.SemiBold)
        Text("Incremento de prioridad base para nuevas tareas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Slider(
            value = manualBoost,
            onValueChange = { manualBoost = it },
            valueRange = 0f..30f,
            steps = 30
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Non-postponable rules
        SectionTitle("Reglas No-Posponibles")
        Text("Desactiva posponer en estas categorías:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Salud -> Medicación")
            Checkbox(checked = nonPostponableHealth, onCheckedChange = { nonPostponableHealth = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Trámites -> Urgente")
            Checkbox(checked = nonPostponableUrgent, onCheckedChange = { nonPostponableUrgent = it })
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { /* TODO: Save settings */ },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Guardar Cambios", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(64.dp)) // padding for bottom nav
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
