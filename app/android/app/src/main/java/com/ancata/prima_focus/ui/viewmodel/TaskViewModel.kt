package com.ancata.prima_focus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.domain.PriorityEngine
import kotlinx.coroutines.Dispatchers
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.ancata.prima_focus.worker.NotificationWorker
import com.ancata.prima_focus.worker.RecurrenceReconciliationWorker
import java.time.LocalDate
import java.util.UUID
import com.ancata.prima_focus.utils.Constants
import com.ancata.prima_focus.utils.RecurrenceCalculator
import com.ancata.prima_focus.utils.TimeUtils
import com.ancata.prima_focus.sync.P2PSyncManager

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ancata.prima_focus.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlin.math.abs

data class FocusDisplayState(
    val heroTask: TaskEntity? = null,
    val secondaryTasks: List<TaskEntity> = emptyList(),
    val tiedTasks: List<TaskEntity> = emptyList(),
    val totalPendingCount: Int = 0
)

sealed interface BackupRestoreState {
    object Idle : BackupRestoreState
    object Loading : BackupRestoreState
    data class Success(val message: String) : BackupRestoreState
    data class Error(val message: String) : BackupRestoreState
}

data class CompletedTaskUiModel(
    val taskId: String,
    val title: String,
    val description: String?,
    val category: String,
    val subcategory: String?,
    val completedAt: Long,
    val formattedDate: String,
    val durationMinutes: Int?,
    val feelingEmoji: String,
    val result: String?,
    val recurrence: String?,
    val rawTask: TaskEntity
)

data class BackupDataPayload(
    val version: Int = 1,
    val app: String = "Prima-Focus",
    val exportedAt: Long = System.currentTimeMillis(),
    val tasks: List<TaskEntity>,
    val sessions: List<SessionEntity>
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE)

    var notificationFrequency: Int
        get() {
            val freq = sharedPrefs.getInt(Constants.PREF_NOTIFICATION_FREQUENCY, 90)
            return if (freq !in listOf(-1, 90, 180, 300)) 90 else freq
        }
        set(value) {
            sharedPrefs.edit().putInt(Constants.PREF_NOTIFICATION_FREQUENCY, value).apply()
            updateNotificationWorker(value)
        }

    var isDisconnectModeEnabled: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_DISCONNECT_MODE_ENABLED, false)
        set(value) {
            sharedPrefs.edit().putBoolean(Constants.PREF_DISCONNECT_MODE_ENABLED, value).apply()
            updateNotificationWorker(notificationFrequency)
        }

    var disconnectStartTime: String
        get() = sharedPrefs.getString(Constants.PREF_DISCONNECT_START_TIME, "22:00") ?: "22:00"
        set(value) {
            sharedPrefs.edit().putString(Constants.PREF_DISCONNECT_START_TIME, value).apply()
            updateNotificationWorker(notificationFrequency)
        }

    var disconnectEndTime: String
        get() = sharedPrefs.getString(Constants.PREF_DISCONNECT_END_TIME, "08:00") ?: "08:00"
        set(value) {
            sharedPrefs.edit().putString(Constants.PREF_DISCONNECT_END_TIME, value).apply()
            updateNotificationWorker(notificationFrequency)
        }

    private fun updateNotificationWorker(frequencyMinutes: Int) {
        val workManager = WorkManager.getInstance(getApplication())
        if (frequencyMinutes <= 0) {
            workManager.cancelUniqueWork(Constants.WORKER_NOTIFICATION)
            return
        }
        
        var initialDelayMinutes = 0L
        if (isDisconnectModeEnabled) {
            initialDelayMinutes = TimeUtils.getMinutesUntilQuietHoursEnd(disconnectStartTime, disconnectEndTime)
        }

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(frequencyMinutes.toLong(), TimeUnit.MINUTES)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            Constants.WORKER_NOTIFICATION,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    var manualBoostAmount: Double
        get() = sharedPrefs.getFloat(Constants.PREF_MANUAL_BOOST_AMOUNT, 10.0f).toDouble()
        set(value) = sharedPrefs.edit().putFloat(Constants.PREF_MANUAL_BOOST_AMOUNT, value.toFloat()).apply()

    var autoSplit: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_AUTO_SPLIT, false)
        set(value) = sharedPrefs.edit().putBoolean(Constants.PREF_AUTO_SPLIT, value).apply()

    var nonPostponableHealth: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_NON_POSTPONABLE_HEALTH, true)
        set(value) = sharedPrefs.edit().putBoolean(Constants.PREF_NON_POSTPONABLE_HEALTH, value).apply()

    var nonPostponableUrgent: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_NON_POSTPONABLE_URGENT, true)
        set(value) = sharedPrefs.edit().putBoolean(Constants.PREF_NON_POSTPONABLE_URGENT, value).apply()

    private val _isHistoryTrackingEnabled = MutableStateFlow(
        sharedPrefs.getBoolean(Constants.PREF_HISTORY_TRACKING_ENABLED, true)
    )
    val isHistoryTrackingEnabled: StateFlow<Boolean> = _isHistoryTrackingEnabled.asStateFlow()

    var isHistoryTrackingEnabledPref: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_HISTORY_TRACKING_ENABLED, true)
        set(value) {
            sharedPrefs.edit().putBoolean(Constants.PREF_HISTORY_TRACKING_ENABLED, value).apply()
            _isHistoryTrackingEnabled.value = value
        }

    val categoriesData = mapOf(
        "trabajo" to listOf("comunicación" to 2.0, "entrega" to 3.5, "tarea adicional" to 2.0, "administrativo" to 1.0, "revisión" to 1.0, "documentación" to 2.0),
        "salud" to listOf("medicación" to 4.0, "cita médica" to 4.0),
        "amigos" to listOf("salud" to 4.0, "comunicación" to 2.0, "favores" to 2.0, "reuniones" to 1.0),
        "pareja" to listOf("salud" to 4.0, "comunicación" to 3.5, "favores" to 2.0, "citas" to 3.5),
        "familia" to listOf("salud" to 4.0, "comunicación" to 2.0, "favores" to 2.0, "reuniones" to 2.0),
        "crecimiento personal" to listOf("terapia y salud mental" to 4.0, "aprendizaje" to 1.0, "proyectos" to 2.0),
        "casa" to listOf("coordinación de reparación" to 4.0, "coordinación de mantenimiento" to 1.0, "compras" to 2.0, "quehaceres" to 2.0),
        "trámites" to listOf("urgente" to 4.0, "normal" to 2.0),
        "finanzas" to listOf("pago de cuentas" to 4.0, "revisión de inversiones" to 1.0)
    )

    fun getDisabledCategories(): Set<String> {
        return sharedPrefs.getStringSet(Constants.PREF_DISABLED_CATEGORIES, emptySet()) ?: emptySet()
    }

    fun setCategoryDisabled(category: String, disabled: Boolean) {
        val current = getDisabledCategories().toMutableSet()
        if (disabled) current.add(category) else current.remove(category)
        sharedPrefs.edit().putStringSet(Constants.PREF_DISABLED_CATEGORIES, current).apply()
    }

    private val db = PrimaFocusDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val sessionDao = db.sessionDao()
    private val priorityEngine = PriorityEngine()

    private val _topTask = MutableStateFlow<TaskEntity?>(null)
    val topTask: StateFlow<TaskEntity?> = _topTask.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("Desconectado")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _backupRestoreState = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val backupRestoreState: StateFlow<BackupRestoreState> = _backupRestoreState.asStateFlow()

    fun resetBackupRestoreState() {
        _backupRestoreState.value = BackupRestoreState.Idle
    }

    val p2pSyncManager = P2PSyncManager(
        context = application,
        onDataReceived = { receivedTasks, receivedSessions -> syncMergeData(receivedTasks, receivedSessions) },
        suspendGetLocalData = { Pair(taskDao.getAllTasks(), sessionDao.getAllSessions()) },
        onStatusUpdate = { status -> _syncStatus.value = status }
    )

    private val _pendingTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val pendingTasks: StateFlow<List<TaskEntity>> = _pendingTasks.asStateFlow()

    val focusDisplayState: StateFlow<FocusDisplayState> = _pendingTasks.map { tasks ->
        if (tasks.isEmpty()) {
            FocusDisplayState()
        } else {
            val hero = tasks.firstOrNull()
            val secondary = tasks.drop(1).take(2)
            val referenceScore = secondary.lastOrNull()?.priorityScore ?: hero?.priorityScore ?: 0.0
            val remainingTasks = tasks.drop(1 + secondary.size)
            val tied = remainingTasks.takeWhile { abs(it.priorityScore - referenceScore) < 0.001 }

            FocusDisplayState(
                heroTask = hero,
                secondaryTasks = secondary,
                tiedTasks = tied,
                totalPendingCount = tasks.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusDisplayState())

    private val _calendarSelectedDate = MutableStateFlow<LocalDate?>(null)
    val calendarSelectedDate: StateFlow<LocalDate?> = _calendarSelectedDate.asStateFlow()

    val calendarFilteredTasks = combine(_pendingTasks, _calendarSelectedDate) { tasks, selectedDate ->
        if (selectedDate == null) {
            tasks
        } else {
            val dateStr = selectedDate.toString()
            tasks.filter { it.date == dateStr }
        }
    }

    fun setCalendarSelectedDate(date: LocalDate?) {
        _calendarSelectedDate.value = date
    }

    val completedTasksList: StateFlow<List<CompletedTaskUiModel>> = taskDao.getCompletedTasksWithSessions()
        .map { list ->
            list.map { taskWithSessions ->
                val latestSession = taskWithSessions.sessions.maxByOrNull { it.createdAt }
                val feelingEmoji = when (latestSession?.feeling) {
                    1 -> "😢"
                    5 -> "😄"
                    else -> "😐"
                }
                val duration = latestSession?.durationMinutes
                    ?: taskWithSessions.task.estimatedMinutes

                CompletedTaskUiModel(
                    taskId = taskWithSessions.task.taskId,
                    title = taskWithSessions.task.title,
                    description = taskWithSessions.task.description,
                    category = taskWithSessions.task.category,
                    subcategory = taskWithSessions.task.subcategory,
                    completedAt = taskWithSessions.task.updatedAt,
                    formattedDate = TimeUtils.formatEpochToDisplay(taskWithSessions.task.updatedAt),
                    durationMinutes = duration,
                    feelingEmoji = feelingEmoji,
                    result = latestSession?.result,
                    recurrence = taskWithSessions.task.recurrence,
                    rawTask = taskWithSessions.task
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun uncompleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val restoredTask = task.copy(
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
            val prioritized = priorityEngine.calculatePriority(restoredTask)
            taskDao.updateTask(prioritized)
            updateWidgets()
        }
    }

    fun deleteCompletedTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(taskId)
            sessionDao.deleteSessionsForTask(taskId)
            updateWidgets()
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteCompletedTasks()
            sessionDao.deleteOrphanedSessions()
            updateWidgets()
        }
    }

    init {
        viewModelScope.launch {
            taskDao.getPendingTasksOrderedByPriority().collectLatest { tasks ->
                _pendingTasks.value = tasks
                if (tasks.isNotEmpty()) {
                    _topTask.value = tasks.first()
                } else {
                    _topTask.value = null
                }
            }
        }
        scheduleRecurrenceWorker()
    }

    /** Registers the daily recurrence reconciliation worker (idempotent — safe to call repeatedly). */
    private fun scheduleRecurrenceWorker() {
        val workRequest = PeriodicWorkRequestBuilder<RecurrenceReconciliationWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            Constants.WORKER_RECURRENCE,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun updateWidgets() {
        val appContext = getApplication<Application>().applicationContext
        
        // Update AddTaskWidget
        val addWidgetIntent = android.content.Intent(appContext, com.ancata.prima_focus.widget.AddTaskWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val addWidgetIds = android.appwidget.AppWidgetManager.getInstance(appContext)
            .getAppWidgetIds(android.content.ComponentName(appContext, com.ancata.prima_focus.widget.AddTaskWidgetProvider::class.java))
        addWidgetIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, addWidgetIds)
        appContext.sendBroadcast(addWidgetIntent)

        // Update TopTaskWidget
        val topWidgetIntent = android.content.Intent(appContext, com.ancata.prima_focus.widget.TopTaskWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val topWidgetIds = android.appwidget.AppWidgetManager.getInstance(appContext)
            .getAppWidgetIds(android.content.ComponentName(appContext, com.ancata.prima_focus.widget.TopTaskWidgetProvider::class.java))
        topWidgetIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, topWidgetIds)
        appContext.sendBroadcast(topWidgetIntent)

        // Update TopThreeTasksWidget (Glance)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(com.ancata.prima_focus.widget.TopThreeTasksWidget::class.java)
                glanceIds.forEach { id ->
                    com.ancata.prima_focus.widget.TopThreeTasksWidget().update(appContext, id)
                }
            } catch (e: Throwable) {
                // Ignore if widget is not placed
            }
        }
    }

    fun quickAdd(
        title: String,
        category: String = "General",
        subcategory: String? = null,
        weight: Double = 2.0,
        estimatedMinutes: Int? = null,
        date: String? = null,
        description: String? = null,
        recurrence: String? = null
    ) {
        val now = System.currentTimeMillis()
        val newTaskId = UUID.randomUUID().toString()
        // If recurring, use the taskId as the group anchor for this series
        val groupId = if (recurrence != null) newTaskId else null
        val newTask = TaskEntity(
            taskId = newTaskId,
            title = title,
            description = description?.trim()?.ifEmpty { null },
            category = category,
            subcategory = subcategory,
            categoryWeight = weight,
            date = date,
            time = null,
            estimatedMinutes = estimatedMinutes,
            isProject = false,
            status = "pending",
            createdAt = now,
            updatedAt = now,
            meta = null,
            postponedReason = null,
            recurrence = recurrence,
            recurrenceGroupId = groupId
        )

        // Pass through engine
        val processedTask = priorityEngine.calculatePriority(newTask)

        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(processedTask)
            updateWidgets()
        }
    }

    private val taskCompletionUseCase = com.ancata.prima_focus.domain.TaskCompletionUseCase(taskDao, sessionDao, priorityEngine)

    fun completeTask(taskId: String, feeling: Int, result: String) {
        viewModelScope.launch(Dispatchers.IO) {
            taskCompletionUseCase.execute(taskId, feeling, result)
            updateWidgets()
        }
    }

    /**
     * Attempts to postpone a task.
     * Invokes onResult with false if the task is blocked by nonPostponable rules, true if applied.
     */
    fun postponeTask(taskId: String, reason: String, onResult: (Boolean) -> Unit = {}) {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            if (task == null) {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }
            val isBlockedByTask = task.nonPostponable
            val isBlockedByHealth = task.category == "salud" &&
                task.subcategory == "medicaci\u00f3n" &&
                nonPostponableHealth
            val isBlockedByUrgent = task.category == "tr\u00e1mites" &&
                task.subcategory == "urgente" &&
                nonPostponableUrgent

            if (isBlockedByTask || isBlockedByHealth || isBlockedByUrgent) {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(false) }
                return@launch
            }

            taskDao.updateTask(task.copy(
                status = "pending",
                postponedReason = reason,
                updatedAt = now
            ))
            updateWidgets()
            kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(true) }
        }
    }

    fun boostTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val boostAmount = manualBoostAmount
                val newBoost = (it.manualBoost + boostAmount).coerceAtMost(50.0)
                val updatedTask = priorityEngine.calculatePriority(it.copy(manualBoost = newBoost))
                taskDao.updateTask(updatedTask)
                updateWidgets()
            }
        }
    }

    fun demoteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val boostAmount = manualBoostAmount
                val newBoost = (it.manualBoost - boostAmount).coerceAtLeast(-50.0)
                val updatedTask = priorityEngine.calculatePriority(it.copy(manualBoost = newBoost))
                taskDao.updateTask(updatedTask)
                updateWidgets()
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(taskId)
            updateWidgets()
        }
    }

    fun deleteSeries(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteTasksByGroupId(groupId)
            updateWidgets()
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupRestoreState.value = BackupRestoreState.Loading
            try {
                val tasks = taskDao.getAllTasks()
                val sessions = sessionDao.getAllSessions()
                val payload = BackupDataPayload(
                    version = 1,
                    app = "Prima-Focus",
                    exportedAt = System.currentTimeMillis(),
                    tasks = tasks,
                    sessions = sessions
                )
                val json = GsonBuilder().setPrettyPrinting().create().toJson(payload)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { writer -> writer.write(json) }
                }
                _backupRestoreState.value = BackupRestoreState.Success("Respaldo exportado correctamente (${tasks.size} tareas, ${sessions.size} sesiones)")
            } catch (e: Exception) {
                _backupRestoreState.value = BackupRestoreState.Error("Error al exportar respaldo: ${e.localizedMessage ?: "desconocido"}")
            }
        }
    }

    fun importBackup(uri: Uri, mergeMode: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _backupRestoreState.value = BackupRestoreState.Loading
            try {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

                val payload = Gson().fromJson(json, BackupDataPayload::class.java)
                    ?: throw IllegalArgumentException("El archivo no tiene un formato válido de Prima-Focus")

                if (payload.app != "Prima-Focus" || payload.tasks == null) {
                    throw IllegalArgumentException("El archivo no pertenece a Prima-Focus")
                }

                val processedTasks = (payload.tasks ?: emptyList()).map { task ->
                    if (task.status == "pending") priorityEngine.calculatePriority(task) else task
                }

                if (!mergeMode) {
                    taskDao.clearAllTasks()
                    sessionDao.clearAllSessions()
                    taskDao.insertTasks(processedTasks)
                    if (!payload.sessions.isNullOrEmpty()) {
                        sessionDao.insertSessions(payload.sessions)
                    }
                } else {
                    val localTasks = taskDao.getAllTasks().associateBy { it.taskId }
                    processedTasks.forEach { received ->
                        val local = localTasks[received.taskId]
                        if (local == null) {
                            taskDao.insertTask(received)
                        } else if (received.updatedAt > local.updatedAt) {
                            taskDao.updateTask(received)
                        }
                    }
                    if (!payload.sessions.isNullOrEmpty()) {
                        sessionDao.insertSessions(payload.sessions)
                    }
                }

                updateWidgets()
                _backupRestoreState.value = BackupRestoreState.Success("Respaldo restaurado con éxito (${processedTasks.size} tareas)")
            } catch (e: Exception) {
                _backupRestoreState.value = BackupRestoreState.Error("Error al restaurar respaldo: ${e.localizedMessage ?: "formato inválido"}")
            }
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(task)
            updateWidgets()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // If the task now has a recurrence and no groupId yet, assign one
            val withGroup = if (task.recurrence != null && task.recurrenceGroupId == null) {
                task.copy(recurrenceGroupId = task.taskId)
            } else if (task.recurrence == null) {
                task.copy(recurrenceGroupId = null)
            } else {
                task
            }
            val finalTask = priorityEngine.calculatePriority(withGroup.copy(updatedAt = System.currentTimeMillis()))
            taskDao.updateTask(finalTask)
            updateWidgets()
        }
    }

    fun splitTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val part1 = it.copy(
                    taskId = java.util.UUID.randomUUID().toString(),
                    title = "[Parte 1] ${it.title}",
                    estimatedMinutes = null,
                    isProject = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "pending"
                )
                
                val part2 = it.copy(
                    taskId = java.util.UUID.randomUUID().toString(),
                    title = "[Parte 2] ${it.title}",
                    estimatedMinutes = null,
                    isProject = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "pending"
                )
                
                taskDao.insertTask(priorityEngine.calculatePriority(part1))
                taskDao.insertTask(priorityEngine.calculatePriority(part2))
                
                taskDao.updateTask(it.copy(status = "archived", updatedAt = System.currentTimeMillis()))
                updateWidgets()
            }
        }
    }

    fun snoozeTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val tomorrow = java.time.LocalDate.now().plusDays(1).toString()
                val updatedTask = it.copy(
                    date = tomorrow,
                    status = "pending",
                    updatedAt = System.currentTimeMillis()
                )
                taskDao.updateTask(priorityEngine.calculatePriority(updatedTask))
                updateWidgets()
            }
        }
    }

    private fun syncMergeData(receivedTasks: List<TaskEntity>, receivedSessions: List<SessionEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val localTasks = taskDao.getAllTasks().associateBy { it.taskId }
            
            receivedTasks.forEach { received ->
                val local = localTasks[received.taskId]
                val processed = if (received.status == "pending") priorityEngine.calculatePriority(received) else received
                if (local == null) {
                    // Task doesn't exist locally, insert it
                    taskDao.insertTask(processed)
                } else {
                    // Task exists locally, Last-Write-Wins based on updatedAt
                    if (received.updatedAt > local.updatedAt) {
                        taskDao.updateTask(processed)
                    }
                }
            }

            if (receivedSessions.isNotEmpty()) {
                val localSessions = sessionDao.getAllSessions().associateBy { it.sessionId }
                val sessionsToUpsert = receivedSessions.filter { received ->
                    val local = localSessions[received.sessionId]
                    local == null || received.updatedAt > local.updatedAt
                }
                if (sessionsToUpsert.isNotEmpty()) {
                    sessionDao.insertSessions(sessionsToUpsert)
                }
            }

            updateWidgets()
        }
    }
}
