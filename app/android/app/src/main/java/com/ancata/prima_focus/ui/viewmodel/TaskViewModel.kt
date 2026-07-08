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
import kotlinx.coroutines.launch
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.ancata.prima_focus.worker.NotificationWorker
import java.util.UUID
import com.ancata.prima_focus.utils.Constants
import com.ancata.prima_focus.utils.TimeUtils

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

    var defaultRecurrence: String
        get() = sharedPrefs.getString(Constants.PREF_DEFAULT_RECURRENCE, "none") ?: "none"
        set(value) = sharedPrefs.edit().putString(Constants.PREF_DEFAULT_RECURRENCE, value).apply()

    var defaultEstimatedMinutes: Int
        get() = sharedPrefs.getInt(Constants.PREF_DEFAULT_ESTIMATED_MINUTES, 15)
        set(value) = sharedPrefs.edit().putInt(Constants.PREF_DEFAULT_ESTIMATED_MINUTES, value).apply()

    var defaultSubtasksCount: Int
        get() = sharedPrefs.getInt(Constants.PREF_DEFAULT_SUBTASKS_COUNT, 0)
        set(value) = sharedPrefs.edit().putInt(Constants.PREF_DEFAULT_SUBTASKS_COUNT, value).apply()

    var autoSplit: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_AUTO_SPLIT, false)
        set(value) = sharedPrefs.edit().putBoolean(Constants.PREF_AUTO_SPLIT, value).apply()

    var nonPostponableHealth: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_NON_POSTPONABLE_HEALTH, true)
        set(value) = sharedPrefs.edit().putBoolean(Constants.PREF_NON_POSTPONABLE_HEALTH, value).apply()

    var nonPostponableUrgent: Boolean
        get() = sharedPrefs.getBoolean(Constants.PREF_NON_POSTPONABLE_URGENT, true)
        set(value) = sharedPrefs.edit().putBoolean(Constants.PREF_NON_POSTPONABLE_URGENT, value).apply()

    val categoriesData = mapOf(
        "trabajo" to listOf("comunicación" to 2.0, "entrega" to 3.5, "tarea adicional" to 2.0, "administrativo" to 1.0, "revisión" to 1.0, "documentación" to 2.0),
        "salud" to listOf("medicación / citas" to 4.0),
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

    private val _pendingTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val pendingTasks: StateFlow<List<TaskEntity>> = _pendingTasks.asStateFlow()

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
    }

    fun quickAdd(
        title: String, 
        category: String = "General", 
        subcategory: String? = null, 
        weight: Double = 2.0,
        estimatedMinutes: Int? = 15,
        date: String? = null,
        subtasksCount: Int = 0
    ) {
        val now = System.currentTimeMillis()
        val tempId = UUID.randomUUID().toString()
        val newTask = TaskEntity(
            taskId = tempId,
            title = title,
            description = null,
            category = category,
            subcategory = subcategory,
            categoryWeight = weight,
            date = date,
            time = null,
            estimatedMinutes = estimatedMinutes,
            subtasksCount = subtasksCount,
            isProject = false,
            status = "pending",
            createdAt = now,
            updatedAt = now,
            meta = null,
            postponedReason = null,
            recurrence = null
        )
        
        // Pass through engine
        val processedTask = priorityEngine.calculatePriority(newTask)
        
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(processedTask)
            updateWidgets()
        }
    }

    fun completeTask(taskId: String, feeling: Int, result: String) {
        val now = System.currentTimeMillis()
        
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                taskDao.updateTask(it.copy(status = "completed", updatedAt = now))
            }
            
            val session = com.ancata.prima_focus.data.local.entity.SessionEntity(
                sessionId = UUID.randomUUID().toString(),
                taskId = taskId,
                startAt = now - (25 * 60 * 1000), 
                endAt = now,
                mode = "focus",
                durationMinutes = 25,
                result = result,
                feeling = feeling,
                createdAt = now,
                updatedAt = now
            )
            sessionDao.insertSession(session)
            updateWidgets()
        }
    }

    fun postponeTask(taskId: String, reason: String) {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                taskDao.updateTask(it.copy(
                    status = "pending",
                    postponedReason = reason,
                    updatedAt = now
                ))
                updateWidgets()
            }
        }
    }

    fun boostTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val boostAmount = manualBoostAmount
                val updatedTask = priorityEngine.calculatePriority(it.copy(manualBoost = it.manualBoost + boostAmount))
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

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(task)
            updateWidgets()
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalTask = priorityEngine.calculatePriority(task.copy(updatedAt = System.currentTimeMillis()))
            taskDao.updateTask(finalTask)
            updateWidgets()
        }
    }

    fun splitTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val halfTime = (it.estimatedMinutes ?: 0) / 2
                
                val part1 = it.copy(
                    taskId = java.util.UUID.randomUUID().toString(),
                    title = "[Parte 1] ${it.title}",
                    estimatedMinutes = halfTime,
                    isProject = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "pending"
                )
                
                val part2 = it.copy(
                    taskId = java.util.UUID.randomUUID().toString(),
                    title = "[Parte 2] ${it.title}",
                    estimatedMinutes = halfTime,
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
}
