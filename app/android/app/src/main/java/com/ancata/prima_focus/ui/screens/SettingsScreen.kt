package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val scrollState = rememberScrollState()
    val glows = LocalPremiumGlows.current

    var manualBoost by remember { mutableFloatStateOf(viewModel.manualBoostAmount.toFloat()) }
    var autoSplit by remember { mutableStateOf(viewModel.autoSplit) }
    var nonPostponableHealth by remember { mutableStateOf(viewModel.nonPostponableHealth) }
    var nonPostponableUrgent by remember { mutableStateOf(viewModel.nonPostponableUrgent) }
    var defaultRecurrence by remember { mutableStateOf(viewModel.defaultRecurrence) }
    var notificationFrequency by remember { mutableIntStateOf(viewModel.notificationFrequency) }
    var defaultEstimatedMinutes by remember { mutableIntStateOf(viewModel.defaultEstimatedMinutes) }
    var defaultSubtasksCount by remember { mutableIntStateOf(viewModel.defaultSubtasksCount) }

    val glassModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(glows.glassSurface)
        .border(
            1.dp,
            Brush.linearGradient(listOf(glows.glassBorderStart, glows.glassBorderEnd)),
            RoundedCornerShape(24.dp)
        )
        .padding(24.dp)

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = glows.primaryAccent,
        checkedTrackColor = glows.primaryGlow.copy(alpha = 0.3f),
        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
        uncheckedTrackColor = glows.glassSurface.copy(alpha = 0.5f),
        uncheckedBorderColor = Color.Transparent
    )
    
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = glows.primaryAccent,
        uncheckedColor = glows.glassBorderStart,
        checkmarkColor = Color.White
    )

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
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
            )

            // Recurrence section placeholder
            Column(modifier = glassModifier) {
                SectionTitle("Recurrencia por defecto")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = defaultRecurrence == "daily", 
                        onClick = { defaultRecurrence = "daily" }, 
                        label = { Text("Diaria") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = glows.glassBorderStart,
                            enabled = true,
                            selected = defaultRecurrence == "daily"
                        )
                    )
                    FilterChip(
                        selected = defaultRecurrence == "weekly", 
                        onClick = { defaultRecurrence = "weekly" }, 
                        label = { Text("Semanal") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = glows.glassBorderStart,
                            enabled = true,
                            selected = defaultRecurrence == "weekly"
                        )
                    )
                    FilterChip(
                        selected = defaultRecurrence == "monthly", 
                        onClick = { defaultRecurrence = "monthly" }, 
                        label = { Text("Mensual") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = glows.glassBorderStart,
                            enabled = true,
                            selected = defaultRecurrence == "monthly"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Valores por defecto
            Column(modifier = glassModifier) {
                SectionTitle("Valores por Defecto de Creación")
                
                Text("Minutos Estimados", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timeOptions = listOf(0 to "∞", 15 to "15m", 25 to "25m", 45 to "45m", 60 to "1h")
                    timeOptions.forEach { (minutes, label) ->
                        FilterChip(
                            selected = defaultEstimatedMinutes == minutes,
                            onClick = { defaultEstimatedMinutes = minutes },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = glows.glassBorderStart,
                                enabled = true,
                                selected = defaultEstimatedMinutes == minutes
                            )
                        )
                    }
                }
                
                Text("Subtareas Automáticas", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val taskOptions = listOf(0 to "0", 1 to "1", 2 to "2", 3 to "3")
                    taskOptions.forEach { (count, label) ->
                        FilterChip(
                            selected = defaultSubtasksCount == count,
                            onClick = { defaultSubtasksCount = count },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = glows.glassBorderStart,
                                enabled = true,
                                selected = defaultSubtasksCount == count
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Frecuencia de Notificaciones
            Column(modifier = glassModifier) {
                SectionTitle("Frecuencia de Notificaciones")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(-1 to "Apagadas", 15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h")
                    options.forEach { (minutes, label) ->
                        FilterChip(
                            selected = notificationFrequency == minutes,
                            onClick = { notificationFrequency = minutes },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = glows.glassBorderStart,
                                enabled = true,
                                selected = notificationFrequency == minutes
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Priority Settings
            Column(modifier = glassModifier) {
                SectionTitle("Motor de Prioridades")
                
                // Auto-split
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text("Dividir automáticamente", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Sugerir dividir tareas largas (>120m)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Switch(checked = autoSplit, onCheckedChange = { autoSplit = it }, colors = switchColors)
                }
                
                HorizontalDivider(color = glows.glassBorderStart, modifier = Modifier.padding(vertical = 12.dp))
                
                // Manual Boost
                Text("Boost Manual: ${manualBoost.toInt()}", fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Puntos de prioridad que se sumarán al presionar el botón de Boost manual en la tarea.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                Slider(
                    value = manualBoost,
                    onValueChange = { manualBoost = it },
                    valueRange = 0f..30f,
                    steps = 30,
                    colors = SliderDefaults.colors(
                        thumbColor = glows.primaryAccent,
                        activeTrackColor = glows.primaryGlow,
                        inactiveTrackColor = glows.glassSurface
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Non-postponable rules
            Column(modifier = glassModifier) {
                SectionTitle("Reglas No-Posponibles")
                Text("Desactiva posponer en estas categorías:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Salud -> Medicación", color = Color.White)
                    Checkbox(checked = nonPostponableHealth, onCheckedChange = { nonPostponableHealth = it }, colors = checkboxColors)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Trámites -> Urgente", color = Color.White)
                    Checkbox(checked = nonPostponableUrgent, onCheckedChange = { nonPostponableUrgent = it }, colors = checkboxColors)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Active Categories
            Column(modifier = glassModifier) {
                SectionTitle("Categorías Activas")
                Text("Oculta las categorías que no utilizas en el menú de Inbox.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 16.dp))
                
                val disabledCats = viewModel.getDisabledCategories()
                viewModel.categoriesData.keys.forEach { categoryName ->
                    var isEnabled by remember { mutableStateOf(!disabledCats.contains(categoryName)) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(categoryName.replaceFirstChar { it.uppercase() }, color = Color.White)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { 
                                isEnabled = it
                                viewModel.setCategoryDisabled(categoryName, !it) 
                            },
                            colors = switchColors
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    viewModel.manualBoostAmount = manualBoost.toDouble()
                    viewModel.autoSplit = autoSplit
                    viewModel.nonPostponableHealth = nonPostponableHealth
                    viewModel.nonPostponableUrgent = nonPostponableUrgent
                    viewModel.defaultRecurrence = defaultRecurrence
                    viewModel.notificationFrequency = notificationFrequency
                    viewModel.defaultEstimatedMinutes = defaultEstimatedMinutes
                    viewModel.defaultSubtasksCount = defaultSubtasksCount
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = glows.primaryAccent),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(96.dp)) // padding for bottom nav FAB
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    val glows = LocalPremiumGlows.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = glows.primaryAccent
    )
}
