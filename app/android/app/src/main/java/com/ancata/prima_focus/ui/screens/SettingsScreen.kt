package com.ancata.prima_focus.ui.screens

import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancata.prima_focus.data.prefs.ThemeMode
import com.ancata.prima_focus.ui.components.EmojiPickerDialog
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.BackupRestoreState
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

/** Every control applies its change immediately; there is no save step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val scrollState = rememberScrollState()
    val glows = LocalPremiumGlows.current
    val context = LocalContext.current
    val preferences = viewModel.preferences
    val textColor = MaterialTheme.colorScheme.onBackground

    var manualBoost by remember { mutableFloatStateOf(preferences.manualBoostAmount.toFloat()) }
    var nonPostponableHealth by remember { mutableStateOf(preferences.nonPostponableHealth) }
    var nonPostponableUrgent by remember { mutableStateOf(preferences.nonPostponableUrgent) }
    var notificationFrequency by remember { mutableIntStateOf(viewModel.notificationFrequency) }
    var isDisconnectModeEnabled by remember { mutableStateOf(viewModel.isDisconnectModeEnabled) }
    var disconnectStartTime by remember { mutableStateOf(viewModel.disconnectStartTime) }
    var disconnectEndTime by remember { mutableStateOf(viewModel.disconnectEndTime) }
    var skipMissedOccurrences by remember { mutableStateOf(preferences.skipMissedOccurrences) }
    val isHistoryTrackingEnabled by preferences.isHistoryTrackingEnabled.collectAsState()
    val themeSettings by preferences.themeSettings.collectAsState()
    val categoryEmojis by preferences.categoryEmojis.collectAsState()

    var notifExpanded by remember { mutableStateOf(false) }
    var emojiPickerCategory by remember { mutableStateOf<String?>(null) }

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
        uncheckedThumbColor = textColor.copy(alpha = 0.7f),
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
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Ajustes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
            )

            // ==================== APPEARANCE ====================
            Column(modifier = glassModifier) {
                SectionTitle("Apariencia")
                val modes = listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = themeSettings.mode == mode,
                            onClick = { preferences.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = glows.primaryAccent.copy(alpha = 0.25f),
                                activeContentColor = textColor,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = textColor.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingSwitchRow(
                        title = "Colores de Material You",
                        subtitle = "Usa la paleta de tu fondo de pantalla",
                        checked = themeSettings.dynamicColor,
                        onCheckedChange = { preferences.setDynamicColor(it) },
                        switchColors = switchColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
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
                                text = { Text(label, color = textColor) },
                                onClick = {
                                    notificationFrequency = minutes
                                    viewModel.notificationFrequency = minutes
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
                SectionHint("Pausa los recordatorios durante la noche")

                SettingSwitchRow(
                    title = "Silenciar durante la noche",
                    checked = isDisconnectModeEnabled,
                    onCheckedChange = {
                        isDisconnectModeEnabled = it
                        viewModel.isDisconnectModeEnabled = it
                    },
                    switchColors = switchColors
                )

                if (isDisconnectModeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeField(label = "Desde", time = disconnectStartTime, defaultHour = 22, modifier = Modifier.weight(1f)) {
                            disconnectStartTime = it
                            viewModel.disconnectStartTime = it
                        }
                        TimeField(label = "Hasta", time = disconnectEndTime, defaultHour = 8, modifier = Modifier.weight(1f)) {
                            disconnectEndTime = it
                            viewModel.disconnectEndTime = it
                        }
                    }
                    Text("Los recordatorios se pausarán y recibirás un resumen a la mañana siguiente.", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.5f), modifier = Modifier.padding(top = 16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = glassModifier) {
                SectionTitle("Motor de Prioridades")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Boost Manual: ${manualBoost.toInt()}", fontWeight = FontWeight.SemiBold, color = textColor)
                SectionHint("Puntos de prioridad que se sumarán al presionar el botón de Boost manual en la tarea.")
                Slider(
                    value = manualBoost,
                    onValueChange = { manualBoost = it },
                    onValueChangeFinished = { preferences.manualBoostAmount = manualBoost.toDouble() },
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
                SectionHint("Desactiva posponer en estas categorías:")

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Salud -> Medicación", color = textColor)
                    Checkbox(checked = nonPostponableHealth, onCheckedChange = {
                        nonPostponableHealth = it
                        preferences.nonPostponableHealth = it
                    }, colors = checkboxColors)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Trámites -> Urgente", color = textColor)
                    Checkbox(checked = nonPostponableUrgent, onCheckedChange = {
                        nonPostponableUrgent = it
                        preferences.nonPostponableUrgent = it
                    }, colors = checkboxColors)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== RECURRING TASKS ====================
            Column(modifier = glassModifier) {
                SectionTitle("Tareas Recurrentes")
                SettingSwitchRow(
                    title = "Saltar ocurrencias vencidas",
                    subtitle = if (skipMissedOccurrences)
                        "Si se te pasa una tarea recurrente, la siguiente queda para hoy o después, sin acumular atrasadas."
                    else
                        "Cada ocurrencia se respeta aunque ya esté vencida (se acumulan).",
                    checked = skipMissedOccurrences,
                    onCheckedChange = {
                        skipMissedOccurrences = it
                        preferences.skipMissedOccurrences = it
                    },
                    switchColors = switchColors
                )
                SectionHint("Cada tarea puede definir su propio ajuste al configurar la repetición.")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = glassModifier) {
                SectionTitle("Categorías")
                SectionHint("Toca el emoji para cambiarlo. Oculta las categorías que no utilizas en el menú de Inbox.")

                val disabledCats = preferences.getDisabledCategories()
                viewModel.categoriesData.keys.forEach { categoryName ->
                    var isEnabled by remember { mutableStateOf(!disabledCats.contains(categoryName)) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { emojiPickerCategory = categoryName },
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(categoryEmojis[categoryName] ?: preferences.categoryEmoji(categoryName), fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            categoryName.replaceFirstChar { it.uppercase() },
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                preferences.setCategoryDisabled(categoryName, !it)
                            },
                            colors = switchColors
                        )
                    }
                }
            }

            emojiPickerCategory?.let { category ->
                EmojiPickerDialog(
                    title = "Emoji para ${category.replaceFirstChar { it.uppercase() }}",
                    currentEmoji = preferences.categoryEmoji(category),
                    onEmojiSelected = {
                        preferences.setCategoryEmoji(category, it)
                        emojiPickerCategory = null
                    },
                    onDismiss = { emojiPickerCategory = null }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== HISTORY & REVIEW SETTINGS ====================
            Column(modifier = glassModifier) {
                SectionTitle("Historial y Revisión de Tareas")
                SectionHint("Controla si deseas registrar un historial detallado y responder a la revisión de estado de ánimo al terminar el temporizador.")

                SettingSwitchRow(
                    title = "Activar Historial y Revisión",
                    subtitle = if (isHistoryTrackingEnabled)
                        "Muestra la revisión al terminar el temporizador y habilita la pestaña 'Historial' en la lista de tareas."
                    else
                        "Completa las tareas al instante sin preguntas y oculta el historial para una experiencia sin fricción.",
                    checked = isHistoryTrackingEnabled,
                    onCheckedChange = { preferences.historyTrackingEnabled = it },
                    switchColors = switchColors
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== LOCAL BACKUP & RESTORE (SAF) ====================
            Column(modifier = glassModifier) {
                SectionTitle("Copia de Seguridad Local (SAF)")
                SectionHint("Tus tareas se guardan de forma segura en tu dispositivo y se conservan automáticamente al actualizar la app. También puedes exportar o importar respaldos manuales en formato JSON.")

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
                    when (val state = backupState) {
                        is BackupRestoreState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetBackupRestoreState()
                        }
                        is BackupRestoreState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
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
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = glows.primaryAccent)
                    ) {
                        Text("Exportar JSON", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart)
                    ) {
                        Text("Restaurar JSON", fontSize = 13.sp, color = textColor)
                    }
                }

                if (showImportDialog && pendingImportUri != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showImportDialog = false
                            pendingImportUri = null
                        },
                        title = { Text("Restaurar Copia de Seguridad", color = textColor, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "¿Cómo deseas restaurar los datos?\n\n• Combinar: Agrega las tareas del respaldo manteniendo las actuales.\n• Sobrescribir: Reemplaza todas las tareas y sesiones actuales por las del archivo.",
                                color = textColor.copy(alpha = 0.85f),
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
                SectionHint("Conecta directamente dos teléfonos o tablets Android cercanos mediante Bluetooth y Wi-Fi Direct. Ambos dispositivos deben confirmar el mismo código antes de intercambiar datos.")

                Text(
                    text = "Dispositivo: ${viewModel.p2pSyncManager.deviceDisplayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val syncStatus by viewModel.syncStatus.collectAsState()

                Text("Estado: $syncStatus", color = glows.primaryAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.p2pSyncManager.startAdvertising() },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = glows.primaryGlow.copy(alpha = 0.5f))
                    ) {
                        Text("Ser Anfitrión", fontSize = 12.sp, color = textColor)
                    }
                    Button(
                        onClick = { viewModel.p2pSyncManager.startDiscovery() },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = glows.primaryGlow.copy(alpha = 0.5f))
                    ) {
                        Text("Ser Cliente", fontSize = 12.sp, color = textColor)
                    }
                }

                if (syncStatus != "Desconectado") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.p2pSyncManager.stopAll() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart)
                    ) {
                        Text("Detener Sincronización", fontSize = 12.sp, color = textColor.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchColors: SwitchColors,
    subtitle: String? = null
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = textColor, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.55f), fontSize = 11.sp)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = switchColors)
    }
}

@Composable
private fun TimeField(
    label: String,
    time: String,
    defaultHour: Int,
    modifier: Modifier = Modifier,
    onTimeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val glows = LocalPremiumGlows.current
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
        OutlinedButton(
            onClick = {
                val parts = time.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: defaultHour
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                TimePickerDialog(context, { _, h, m ->
                    onTimeSelected(String.format(java.util.Locale.ROOT, "%02d:%02d", h, m))
                }, hour, minute, true).show()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, glows.glassBorderStart)
        ) {
            Text(time)
        }
    }
}

@Composable
private fun SectionHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
    )
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
