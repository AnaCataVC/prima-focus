package com.ancata.prima_focus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val taskId: String,
    val title: String,
    val description: String?,
    val category: String,
    val subcategory: String?,
    val categoryWeight: Double,
    val date: String?,                 // YYYY-MM-DD or null
    val time: String?,                 // HH:MM or null
    val hasTime: Boolean = false,
    val timeUrgency: Double = 0.0,
    val estimatedMinutes: Int?,
    val subtasksCount: Int = 0,
    val isProject: Boolean = false,
    val recurrence: String?,           // "DAILY" | "WEEKLY:MO,WE,FR" | "MONTHLY" | null
    val recurrenceGroupId: String? = null, // Groups all instances of the same recurring series
    val manualBoost: Double = 0.0,
    val nonPostponable: Boolean = false,
    val priorityScore: Double = 0.0,
    val status: String = "pending",    // pending|in_progress|completed|archived
    val postponedReason: String?,
    val createdAt: Long,               // epoch ms
    val updatedAt: Long,               // epoch ms
    val meta: String?
)
