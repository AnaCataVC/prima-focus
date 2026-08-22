package com.ancata.prima_focus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarWidget(
    selectedDate: LocalDate? = null,
    workloadMap: Map<String, Int> = emptyMap(),
    onDateSelected: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glows = LocalPremiumGlows.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(glows.glassSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(glows.glassBorderStart, glows.glassBorderEnd)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES")).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Row {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = Color.White.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                daysOfWeek.forEach { day ->
                    Text(text = day, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value
            val daysInMonth = currentMonth.lengthOfMonth()
            
            var dayCounter = 1
            var started = false
            
            for (week in 0..5) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (dayOfWeek in 1..7) {
                        if (!started && dayOfWeek == firstDayOfMonth) started = true
                        
                        if (started && dayCounter <= daysInMonth) {
                            val currentDate = currentMonth.atDay(dayCounter)
                            val isToday = currentDate == LocalDate.now()
                            val isSelected = currentDate == selectedDate
                            val workload = workloadMap[currentDate.toString()] ?: 0
                            
                            val cellBackground = when {
                                isSelected -> glows.primaryAccent
                                isToday -> glows.primaryAccent.copy(alpha = 0.25f)
                                workload > 0 -> glows.primaryAccent.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }

                            val cellBorderModifier = if (workload > 0 && !isSelected) {
                                Modifier.border(1.dp, glows.primaryAccent.copy(alpha = 0.4f), CircleShape)
                            } else if (isToday && !isSelected) {
                                Modifier.border(1.dp, glows.primaryAccent.copy(alpha = 0.6f), CircleShape)
                            } else {
                                Modifier
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(cellBackground)
                                    .then(cellBorderModifier)
                                    .clickable { onDateSelected(currentDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayCounter.toString(),
                                        color = if (isSelected || isToday || workload > 0) Color.White else Color.White.copy(alpha = 0.75f),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday || workload > 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (workload > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.Center) {
                                            val dots = workload.coerceAtMost(3)
                                            for (i in 1..dots) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else glows.primaryAccent)
                                                )
                                                if (i < dots) Spacer(modifier = Modifier.width(2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            dayCounter++
                        } else {
                            Spacer(modifier = Modifier.size(38.dp))
                        }
                    }
                }
                if (dayCounter > daysInMonth) break
            }
        }
    }
}
