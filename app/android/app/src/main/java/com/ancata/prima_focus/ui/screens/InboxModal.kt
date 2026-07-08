package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxModal(
    viewModel: TaskViewModel,
    taskToEdit: com.ancata.prima_focus.data.local.entity.TaskEntity? = null,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(taskToEdit?.title ?: "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glows = LocalPremiumGlows.current
    
    val disabledCats = viewModel.getDisabledCategories()
    val categoriesData = viewModel.categoriesData.filterKeys { !disabledCats.contains(it) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var subcategoryExpanded by remember { mutableStateOf(false) }
    
    val categoryNames = categoriesData.keys.toList()
    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: categoryNames.firstOrNull() ?: "") }
    
    val currentSubcategories = categoriesData[selectedCategory] ?: emptyList()
    var selectedSubcategory by remember(selectedCategory) { 
        mutableStateOf(
            if (taskToEdit != null && taskToEdit.category == selectedCategory) {
                currentSubcategories.find { it.first == taskToEdit.subcategory } ?: currentSubcategories.firstOrNull()
            } else {
                currentSubcategories.firstOrNull()
            }
        ) 
    }
    var selectedDate by remember { mutableStateOf(taskToEdit?.date) }
    var estimatedMinutes by remember { mutableStateOf(taskToEdit?.estimatedMinutes?.toString() ?: "") }
    var subtasksCount by remember { mutableStateOf(if (taskToEdit?.subtasksCount != null && taskToEdit.subtasksCount > 0) taskToEdit.subtasksCount.toString() else "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glows.backgroundCenter.copy(alpha = 0.95f), // High opacity for readability
        scrimColor = Color.Black.copy(alpha = 0.7f), // Dark scrim as per UX rules
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Main Input Area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("¿Qué tienes en mente?", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = glows.primaryAccent,
                        unfocusedBorderColor = glows.glassBorderStart,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = glows.primaryAccent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                // Big circular send button
                FloatingActionButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            val subcat = selectedSubcategory?.first
                            val weight = selectedSubcategory?.second ?: 2.0
                            val parsedMinutes = estimatedMinutes.toIntOrNull() ?: viewModel.defaultEstimatedMinutes
                            val finalMinutes = if (parsedMinutes <= 0) null else parsedMinutes
                            val parsedSubtasks = subtasksCount.toIntOrNull() ?: viewModel.defaultSubtasksCount

                            if (taskToEdit != null) {
                                viewModel.updateTask(
                                    taskToEdit.copy(
                                        title = text,
                                        category = selectedCategory,
                                        subcategory = subcat,
                                        categoryWeight = weight,
                                        date = selectedDate,
                                        estimatedMinutes = finalMinutes,
                                        subtasksCount = parsedSubtasks
                                    )
                                )
                            } else {
                                viewModel.quickAdd(
                                    title = text, 
                                    category = selectedCategory, 
                                    subcategory = subcat, 
                                    weight = weight,
                                    date = selectedDate,
                                    estimatedMinutes = finalMinutes,
                                    subtasksCount = parsedSubtasks
                                )
                            }
                            onDismiss()
                        }
                    },
                    shape = CircleShape,
                    containerColor = glows.primaryAccent,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Guardar",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría", color = Color.White.copy(alpha = 0.6f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = glows.primaryGlow,
                            unfocusedBorderColor = glows.glassBorderStart
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(glows.backgroundEdge)
                    ) {
                        categoryNames.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                ExposedDropdownMenuBox(
                    expanded = subcategoryExpanded,
                    onExpandedChange = { subcategoryExpanded = !subcategoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedSubcategory?.first ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subcategoría", color = Color.White.copy(alpha = 0.6f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcategoryExpanded) },
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = glows.primaryGlow,
                            unfocusedBorderColor = glows.glassBorderStart
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = subcategoryExpanded,
                        onDismissRequest = { subcategoryExpanded = false },
                        modifier = Modifier.background(glows.backgroundEdge)
                    ) {
                        currentSubcategories.forEach { subcat ->
                            DropdownMenuItem(
                                text = { Text(subcat.first, color = Color.White) },
                                onClick = {
                                    selectedSubcategory = subcat
                                    subcategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDate == "Hoy",
                    onClick = { selectedDate = if (selectedDate == "Hoy") null else "Hoy" },
                    label = { Text("Hoy") },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, labelColor = Color.White),
                    border = FilterChipDefaults.filterChipBorder(borderColor = glows.glassBorderStart, enabled = true, selected = selectedDate == "Hoy")
                )
                FilterChip(
                    selected = selectedDate == "Mañana",
                    onClick = { selectedDate = if (selectedDate == "Mañana") null else "Mañana" },
                    label = { Text("Mañana") },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, labelColor = Color.White),
                    border = FilterChipDefaults.filterChipBorder(borderColor = glows.glassBorderStart, enabled = true, selected = selectedDate == "Mañana")
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = estimatedMinutes,
                    onValueChange = { estimatedMinutes = it },
                    label = { Text("Minutos (0 = ∞)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = glows.primaryGlow,
                        unfocusedBorderColor = glows.glassBorderStart
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = subtasksCount,
                    onValueChange = { subtasksCount = it },
                    label = { Text("Subtareas (Opc)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = glows.primaryGlow,
                        unfocusedBorderColor = glows.glassBorderStart
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
