package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.utils.RecurrenceCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bottom sheet for configuring a recurrence rule on a task.
 *
 * Supports three patterns:
 *   - No recurrence (null)
 *   - DAILY
 *   - WEEKLY:MO,WE,FR  (weekday chips)
 *   - MONTHLY
 *
 * The sheet is always an *independent* ModalBottomSheet (never nested inside
 * another sheet) to avoid BackHandler conflicts with the parent InboxModal.
 *
 * @param currentRule   The recurrence rule currently set on the task (null = no recurrence).
 * @param isEditing     When true, shows the destructive "Eliminar repeticion" option.
 * @param onRuleSelected Callback with the newly selected rule (null = remove recurrence).
 * @param onDismiss     Called when the sheet should be closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSheet(
    currentRule: String?,
    isEditing: Boolean,
    onRuleSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glows = LocalPremiumGlows.current

    // --- Local state mirroring the selected rule ---
    var selectedFrequency by remember {
        mutableStateOf(
            when {
                currentRule == null -> "NONE"
                currentRule == "DAILY" -> "DAILY"
                currentRule == "MONTHLY" -> "MONTHLY"
                currentRule.startsWith("WEEKLY:") -> "WEEKLY"
                else -> "NONE"
            }
        )
    }

    // Active weekday codes (e.g. setOf("MO", "WE", "FR"))
    var selectedDays by remember {
        mutableStateOf(
            if (currentRule?.startsWith("WEEKLY:") == true) {
                currentRule.removePrefix("WEEKLY:").split(",").map { it.trim() }.toSet()
            } else {
                setOf("MO", "WE", "FR") // sensible default for weekly
            }
        )
    }

    /** Converts local state to a rule string. Returns null for NONE. */
    fun buildRule(): String? = when (selectedFrequency) {
        "DAILY" -> "DAILY"
        "MONTHLY" -> "MONTHLY"
        "WEEKLY" -> if (selectedDays.isNotEmpty()) "WEEKLY:${selectedDays.joinToString(",")}" else null
        else -> null
    }

    /** Preview of the next occurrence date, shown in real time. */
    val previewDate: String? = remember(selectedFrequency, selectedDays) {
        val rule = buildRule() ?: return@remember null
        val next = RecurrenceCalculator.computeNextDate(rule, LocalDate.now()) ?: return@remember null
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.forLanguageTag("es-ES"))
        next.format(formatter)
    }

    val weekdays = listOf(
        "MO" to "Lun",
        "TU" to "Mar",
        "WE" to "Mie",
        "TH" to "Jue",
        "FR" to "Vie",
        "SA" to "Sab",
        "SU" to "Dom"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glows.backgroundCenter.copy(alpha = 0.97f),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repeticion",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                TextButton(
                    onClick = { onRuleSelected(buildRule()); onDismiss() }
                ) {
                    Text("Listo", color = glows.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }

            // Frequency selector (single-select radio-style chips)
            val frequencies = listOf(
                "NONE" to "No repite",
                "DAILY" to "Diario",
                "WEEKLY" to "Semanal",
                "MONTHLY" to "Mensual"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frequencies.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFrequency == key,
                        onClick = { selectedFrequency = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(alpha = 0.7f),
                            selectedContainerColor = glows.primaryAccent.copy(alpha = 0.25f),
                            selectedLabelColor = glows.primaryAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = glows.glassBorderStart,
                            selectedBorderColor = glows.primaryAccent,
                            enabled = true,
                            selected = selectedFrequency == key
                        ),
                        modifier = Modifier.height(36.dp)
                    )
                }
            }

            // Weekday picker (only visible when WEEKLY is selected)
            if (selectedFrequency == "WEEKLY") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekdays.forEach { (code, label) ->
                        val isSelected = code in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected && selectedDays.size > 1) {
                                    selectedDays - code
                                } else if (!isSelected) {
                                    selectedDays + code
                                } else {
                                    selectedDays // prevent deselecting the last day
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Color.White.copy(alpha = 0.6f),
                                selectedContainerColor = glows.primaryAccent.copy(alpha = 0.3f),
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = glows.glassBorderStart,
                                selectedBorderColor = glows.primaryAccent,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .weight(1f)
                        )
                    }
                }
            }

            // Real-time preview of next occurrence
            if (previewDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Proxima: $previewDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // Destructive option: only shown when editing an existing recurring task
            if (isEditing && currentRule != null) {
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { onRuleSelected(null); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Eliminar repeticion",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}
