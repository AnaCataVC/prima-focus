package com.ancata.prima_focus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.ui.components.CalendarWidget
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel

@Composable
fun TabletDashboardScreen(
    viewModel: TaskViewModel,
    snackbarHostState: SnackbarHostState,
    onStartTimer: (String, String, Int) -> Unit = { _, _, _ -> },
    onEditTask: (TaskEntity) -> Unit = {},
    onRequestReview: (String) -> Unit = {}
) {
    val glows = LocalPremiumGlows.current
    val selectedDate by viewModel.calendarSelectedDate.collectAsState()
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    
    // Group tasks by date string and count them
    val workloadMap = remember(pendingTasks) {
        pendingTasks
            .filter { it.date != null }
            .groupBy { it.date!! }
            .mapValues { it.value.size }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(glows.backgroundCenter, glows.backgroundEdge),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                        radius = size.height * 0.8f
                    )
                )
            }
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                    HomeScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onStartTimer = onStartTimer,
                        onEditTask = onEditTask,
                        onRequestReview = onRequestReview
                    )
                }
                Box(modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CalendarWidget(
                        selectedDate = selectedDate,
                        workloadMap = workloadMap,
                        onDateSelected = { date ->
                            if (date == selectedDate) {
                                viewModel.setCalendarSelectedDate(null)
                            } else {
                                viewModel.setCalendarSelectedDate(date)
                            }
                        }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.4f).fillMaxWidth()) {
                    HomeScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onStartTimer = onStartTimer,
                        onEditTask = onEditTask,
                        onRequestReview = onRequestReview
                    )
                }
                Box(modifier = Modifier.weight(0.6f).fillMaxWidth().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CalendarWidget(
                        selectedDate = selectedDate,
                        workloadMap = workloadMap,
                        onDateSelected = { date ->
                            if (date == selectedDate) {
                                viewModel.setCalendarSelectedDate(null)
                            } else {
                                viewModel.setCalendarSelectedDate(date)
                            }
                        }
                    )
                }
            }
        }
    }
}
