package com.ancata.prima_focus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val taskId: String,
    val title: String,
    val description: String? = null,
    val category: String,
    val subcategory: String? = null,
    val categoryWeight: Double,
    val date: String? = null,                 // YYYY-MM-DD or null
    val time: String? = null,                 // HH:MM or null
    val hasTime: Boolean = false,
    val timeUrgency: Double = 0.0,
    val estimatedMinutes: Int? = null,
    val isProject: Boolean = false,
    val recurrence: String? = null,           // "DAILY" | "WEEKLY:MO,WE,FR" | "MONTHLY" | null
    val recurrenceGroupId: String? = null, // Groups all instances of the same recurring series
    val manualBoost: Double = 0.0,
    val nonPostponable: Boolean = false,
    val priorityScore: Double = 0.0,
    val status: String = "pending",    // pending|in_progress|completed|archived
    val postponedReason: String? = null,
    val createdAt: Long,               // epoch ms
    val updatedAt: Long,               // epoch ms
    val meta: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncVersion: Long = 1L
)
