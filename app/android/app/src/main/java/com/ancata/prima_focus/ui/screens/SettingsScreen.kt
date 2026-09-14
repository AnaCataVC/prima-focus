package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ancata.prima_focus.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val scrollState = rememberScrollState()
    val glows = LocalPremiumGlows.current
    val context = LocalContext.current

    var manualBoost by remember { mutableFloatStateOf(viewModel.manualBoostAmount.toFloat()) }
    var nonPostponableHealth by remember { mutableStateOf(viewModel.nonPostponableHealth) }
    var nonPostponableUrgent by remember { mutableStateOf(viewModel.nonPostponableUrgent) }
    var notificationFrequency by remember { mutableIntStateOf(viewModel.notificationFrequency) }
    var isDisconnectModeEnabled by remember { mutableStateOf(viewModel.isDisconnectModeEnabled) }
    var disconnectStartTime by remember { mutableStateOf(viewModel.disconnectStartTime) }
    var disconnectEndTime by remember { mutableStateOf(viewModel.disconnectEndTime) }
    var isHistoryTrackingEnabled by remember { mutableStateOf(viewModel.isHistoryTrackingEnabledPref) }

    val sharedPrefs = remember { context.getSharedPreferences(Constants.PREF_FILE, android.content.Context.MODE_PRIVATE) }
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
                
                Spacer(modifier = Modifier.height(16.dp))

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
            
            Spacer(modifier = Modifier.height(24.dp))

            // ==================== HISTORY & REVIEW SETTINGS ====================
            Column(modifier = glassModifier) {
                SectionTitle("Historial y Revisión de Tareas")
                Text(
                    text = "Controla si deseas registrar un historial detallado y responder a la revisión de estado de ánimo al terminar el temporizador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Activar Historial y Revisión", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isHistoryTrackingEnabled) 
                                "Muestra la revisión al terminar el temporizador y habilita la pestaña 'Historial' en la lista de tareas."
                            else 
                                "Completa las tareas al instante sin preguntas y oculta el historial para una experiencia sin fricción.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isHistoryTrackingEnabled,
                        onCheckedChange = { isHistoryTrackingEnabled = it },
                        colors = switchColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== LOCAL BACKUP & RESTORE (SAF) ====================
            Column(modifier = glassModifier) {
                SectionTitle("Copia de Seguridad Local (SAF)")
                Text(
                    text = "Tus tareas se guardan de forma segura en tu dispositivo y se conservan automáticamente al actualizar la app. También puedes exportar o importar respaldos manuales en formato JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                val backupState by viewModel.backupRestoreState.collectAsState()
                var showImportDialog by remember { mutableStateOf(false) }
                var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

                val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    uri?.let { viewModel.exportBackup(it) }
                }

                val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        pendingImportUri = it
                        showImportDialog = true
                    }
                }

                LaunchedEffect(backupState) {
                    when (backupState) {
                        is com.ancata.prima_focus.ui.viewmodel.BackupRestoreState.Success -> {
                            Toast.makeText(context, (backupState as com.ancata.prima_focus.ui.viewmodel.BackupRestoreState.Success).message, Toast.LENGTH_LONG).show()
                            viewModel.resetBackupRestoreState()
                        }
                        is com.ancata.prima_focus.ui.viewmodel.BackupRestoreState.Error -> {
                            Toast.makeText(context, (backupState as com.ancata.prima_focus.ui.viewmodel.BackupRestoreState.Error).message, Toast.LENGTH_LONG).show()
                            viewModel.resetBackupRestoreState()
                        }
                        else -> {}
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultName = "prima_focus_backup_${java.time.LocalDate.now()}.json"
                            exportLauncher.launch(defaultName)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = glows.primaryAccent)
                    ) {
                        Text("Exportar JSON", fontSize = 13.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart)
                    ) {
                        Text("Restaurar JSON", fontSize = 13.sp, color = Color.White)
                    }
                }

                if (showImportDialog && pendingImportUri != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showImportDialog = false
                            pendingImportUri = null
                        },
                        title = { Text("Restaurar Copia de Seguridad", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "¿Cómo deseas restaurar los datos?\n\n• Combinar: Agrega las tareas del respaldo manteniendo las actuales.\n• Sobrescribir: Reemplaza todas las tareas y sesiones actuales por las del archivo.",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingImportUri?.let { viewModel.importBackup(it, mergeMode = true) }
                                showImportDialog = false
                                pendingImportUri = null
                            }) {
                                Text("Combinar (Recomendado)", color = glows.primaryAccent, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                pendingImportUri?.let { viewModel.importBackup(it, mergeMode = false) }
                                showImportDialog = false
                                pendingImportUri = null
                            }) {
                                Text("Sobrescribir", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        containerColor = glows.backgroundCenter
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = glassModifier) {
                SectionTitle("Móvil a Móvil (Nearby Android)")
                Text("Conecta directamente dos teléfonos o tablets Android cercanos mediante Bluetooth y Wi-Fi Direct.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                
                Text(
                    text = "Dispositivo: ${viewModel.p2pSyncManager.deviceDisplayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val syncStatus by viewModel.syncStatus.collectAsState()
                
                Text("Estado: $syncStatus", color = glows.primaryAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.p2pSyncManager.startAdvertising() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = glows.primaryGlow.copy(alpha = 0.5f))
                    ) {
                        Text("Ser Anfitrión", fontSize = 12.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.p2pSyncManager.startDiscovery() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = glows.primaryGlow.copy(alpha = 0.5f))
                    ) {
                        Text("Ser Cliente", fontSize = 12.sp, color = Color.White)
                    }
                }

                if (syncStatus != "Desconectado") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.p2pSyncManager.stopAll() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    ) {
                        Text("Detener Sincronización", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    viewModel.manualBoostAmount = manualBoost.toDouble()
                    viewModel.nonPostponableHealth = nonPostponableHealth
                    viewModel.nonPostponableUrgent = nonPostponableUrgent
                    viewModel.notificationFrequency = notificationFrequency

                    viewModel.isDisconnectModeEnabled = isDisconnectModeEnabled
                    viewModel.disconnectStartTime = disconnectStartTime
                    viewModel.disconnectEndTime = disconnectEndTime
                    viewModel.isHistoryTrackingEnabledPref = isHistoryTrackingEnabled

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
