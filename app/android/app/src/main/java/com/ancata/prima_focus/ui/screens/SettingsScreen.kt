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
import androidx.compose.ui.platform.LocalContext
import android.app.TimePickerDialog
import android.widget.Toast
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
    val context = LocalContext.current

    var manualBoost by remember { mutableFloatStateOf(viewModel.manualBoostAmount.toFloat()) }
    var autoSplit by remember { mutableStateOf(viewModel.autoSplit) }
    var nonPostponableHealth by remember { mutableStateOf(viewModel.nonPostponableHealth) }
    var nonPostponableUrgent by remember { mutableStateOf(viewModel.nonPostponableUrgent) }
    var notificationFrequency by remember { mutableIntStateOf(viewModel.notificationFrequency) }
    var isDisconnectModeEnabled by remember { mutableStateOf(viewModel.isDisconnectModeEnabled) }
    var disconnectStartTime by remember { mutableStateOf(viewModel.disconnectStartTime) }
    var disconnectEndTime by remember { mutableStateOf(viewModel.disconnectEndTime) }

    var notifExpanded by remember { mutableStateOf(false) }

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


            
            Column(modifier = glassModifier) {
                SectionTitle("Frecuencia de Notificaciones")
                
                val options = listOf(-1 to "Apagadas", 90 to "1 hora y media", 180 to "3 horas", 300 to "5 horas")
                val currentLabel = options.find { it.first == notificationFrequency }?.second ?: "Apagadas"
                
                ExposedDropdownMenuBox(
                    expanded = notifExpanded,
                    onExpandedChange = { notifExpanded = !notifExpanded },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = notifExpanded) },
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = glows.primaryGlow,
                            unfocusedBorderColor = glows.glassBorderStart,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = notifExpanded,
                        onDismissRequest = { notifExpanded = false },
                        modifier = Modifier.background(glows.backgroundEdge)
                    ) {
                        options.forEach { (minutes, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = {
                                    notificationFrequency = minutes
                                    notifExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = glassModifier) {
                SectionTitle("Modo Desconexión")
                Text("Pausa los recordatorios durante la noche", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Silenciar durante la noche", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isDisconnectModeEnabled, onCheckedChange = { isDisconnectModeEnabled = it }, colors = switchColors)
                }
                
                if (isDisconnectModeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Desde", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            OutlinedButton(
                                onClick = {
                                    val parts = disconnectStartTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, hour, minute ->
                                        disconnectStartTime = String.format("%02d:%02d", hour, minute)
                                    }, h, m, true).show()
                                },
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart)
                            ) {
                                Text(disconnectStartTime)
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hasta", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            OutlinedButton(
                                onClick = {
                                    val parts = disconnectEndTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, hour, minute ->
                                        disconnectEndTime = String.format("%02d:%02d", hour, minute)
                                    }, h, m, true).show()
                                },
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart)
                            ) {
                                Text(disconnectEndTime)
                            }
                        }
                    }
                    Text("Los recordatorios se pausarán y recibirás un resumen a la mañana siguiente.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = glassModifier) {
                SectionTitle("Motor de Prioridades")
                
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
                    viewModel.notificationFrequency = notificationFrequency

                    viewModel.isDisconnectModeEnabled = isDisconnectModeEnabled
                    viewModel.disconnectStartTime = disconnectStartTime
                    viewModel.disconnectEndTime = disconnectEndTime

                    
                    Toast.makeText(context, "Ajustes guardados correctamente", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = glows.primaryAccent),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(96.dp))
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
