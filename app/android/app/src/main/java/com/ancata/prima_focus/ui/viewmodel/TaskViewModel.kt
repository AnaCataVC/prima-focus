package com.ancata.prima_focus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ancata.prima_focus.data.local.PrimaFocusDatabase
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.domain.PriorityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

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

    fun quickAdd(title: String, category: String = "General", weight: Double = 2.0) {
        val now = System.currentTimeMillis()
        val tempId = "task_$now"
        val newTask = TaskEntity(
            taskId = tempId,
            title = title,
            description = null,
            category = category,
            subcategory = null,
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
}
