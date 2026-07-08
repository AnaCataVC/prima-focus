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

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("prima_focus_prefs", Context.MODE_PRIVATE)

    var notificationFrequency: Int
        get() = sharedPrefs.getInt("notification_frequency", 15)
        set(value) {
            sharedPrefs.edit().putInt("notification_frequency", value).apply()
            updateNotificationWorker(value)
        }

    private fun updateNotificationWorker(frequencyMinutes: Int) {
        val workManager = WorkManager.getInstance(getApplication())
        if (frequencyMinutes <= 0) {
            workManager.cancelUniqueWork("NotificationWorker")
            return
        }
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(frequencyMinutes.toLong(), TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            "NotificationWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    var manualBoostAmount: Double
        get() = sharedPrefs.getFloat("manual_boost_amount", 10.0f).toDouble()
        set(value) = sharedPrefs.edit().putFloat("manual_boost_amount", value.toFloat()).apply()

    var defaultRecurrence: String
        get() = sharedPrefs.getString("default_recurrence", "none") ?: "none"
        set(value) = sharedPrefs.edit().putString("default_recurrence", value).apply()

    var defaultEstimatedMinutes: Int
        get() = sharedPrefs.getInt("default_estimated_minutes", 15)
        set(value) = sharedPrefs.edit().putInt("default_estimated_minutes", value).apply()

    var defaultSubtasksCount: Int
        get() = sharedPrefs.getInt("default_subtasks_count", 0)
        set(value) = sharedPrefs.edit().putInt("default_subtasks_count", value).apply()

    var autoSplit: Boolean
        get() = sharedPrefs.getBoolean("auto_split", false)
        set(value) = sharedPrefs.edit().putBoolean("auto_split", value).apply()

    var nonPostponableHealth: Boolean
        get() = sharedPrefs.getBoolean("non_postponable_health", true)
        set(value) = sharedPrefs.edit().putBoolean("non_postponable_health", value).apply()

    var nonPostponableUrgent: Boolean
        get() = sharedPrefs.getBoolean("non_postponable_urgent", true)
        set(value) = sharedPrefs.edit().putBoolean("non_postponable_urgent", value).apply()

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
        return sharedPrefs.getStringSet("disabled_categories", emptySet()) ?: emptySet()
    }

    fun setCategoryDisabled(category: String, disabled: Boolean) {
        val current = getDisabledCategories().toMutableSet()
        if (disabled) current.add(category) else current.remove(category)
        sharedPrefs.edit().putStringSet("disabled_categories", current).apply()
    }

    private val db = PrimaFocusDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val sessionDao = db.sessionDao()
    private val priorityEngine = PriorityEngine()

    private val _topTask = MutableStateFlow<TaskEntity?>(null)
    val topTask: StateFlow<TaskEntity?> = _topTask.asStateFlow()

    init {
        viewModelScope.launch {
            taskDao.getPendingTasksOrderedByPriority().collectLatest { tasks ->
                if (tasks.isNotEmpty()) {
                    _topTask.value = tasks.first()
                } else {
                    _topTask.value = null
                }
            }
        }
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
        val isProject = (estimatedMinutes ?: 0) > 120
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
            isProject = isProject,
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
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(taskId)
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalTask = priorityEngine.calculatePriority(task.copy(updatedAt = System.currentTimeMillis()))
            taskDao.updateTask(finalTask)
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
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "pending"
                )
                
                val part2 = it.copy(
                    taskId = java.util.UUID.randomUUID().toString(),
                    title = "[Parte 2] ${it.title}",
                    estimatedMinutes = halfTime,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "pending"
                )
                
                taskDao.insertTask(priorityEngine.calculatePriority(part1))
                taskDao.insertTask(priorityEngine.calculatePriority(part2))
                
                taskDao.updateTask(it.copy(status = "archived", updatedAt = System.currentTimeMillis()))
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
            }
        }
    }
}
