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

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("prima_focus_prefs", Context.MODE_PRIVATE)

    fun getManualBoostAmount(): Double {
        return sharedPrefs.getFloat("manual_boost_amount", 10.0f).toDouble()
    }

    fun setManualBoostAmount(amount: Float) {
        sharedPrefs.edit().putFloat("manual_boost_amount", amount).apply()
    }

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
        // Observers from DB
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

    fun quickAdd(title: String, category: String = "General", subcategory: String? = null, weight: Double = 2.0) {
        val now = System.currentTimeMillis()
        val tempId = "task_$now"
        val newTask = TaskEntity(
            taskId = tempId,
            title = title,
            description = null,
            category = category,
            subcategory = subcategory,
            categoryWeight = weight,
            date = null,
            time = null,
            estimatedMinutes = 15,
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
                sessionId = "session_$now",
                taskId = taskId,
                startAt = now - (25 * 60 * 1000), // Approximate for MVP
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
                val boostAmount = getManualBoostAmount()
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
}
