package com.primafocus.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["date", "status", "priorityScore"], name = "idx_tasks_today_candidate"),
        Index(value = ["updatedAt"], name = "idx_tasks_updatedAt")
    ]
)
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val description: String?,
    val category: String,
    val subcategory: String?,
    val categoryWeight: Double,
    val date: String?, // YYYY-MM-DD
    val time: String?, // HH:MM
    val hasTime: Boolean = false,
    val timeUrgency: Double = 0.0,
    val estimatedMinutes: Int? = null,
    val subtasksCount: Int = 0,
    val isProject: Boolean = false,
    val recurrence: String? = null,
    val manualBoost: Double = 5.0,
    val nonPostponable: Boolean = false,
    val priorityScore: Double = 0.0,
    val status: String = "pending",
    val posponedReason: String? = null,
    val encrypted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Int = 1,
    val dirty: Boolean = true,
    val meta: String? = null
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["taskId"], name = "idx_sessions_taskId")
    ]
)
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val taskId: String?,
    val userId: String?,
    val startAt: Long,
    val endAt: Long?,
    val mode: String?,
    val durationMinutes: Int?,
    val result: String?,
    val feeling: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val dirty: Boolean = true
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String,
    val taskId: String?,
    val type: String?,
    val scheduledAt: Long?,
    val attempt: Int = 0,
    val action: String?,
    val status: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val dirty: Boolean = true
)

@Entity(tableName = "analytics")
data class AnalyticsEntity(
    @PrimaryKey val eventId: String,
    val eventType: String?,
    val payload: String?,
    val createdAt: Long
)
