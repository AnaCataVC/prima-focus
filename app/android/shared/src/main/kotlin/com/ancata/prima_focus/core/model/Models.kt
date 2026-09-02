package com.ancata.prima_focus.core.model

data class Task(
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
    val recurrenceGroupId: String? = null,    // Groups all instances of the same recurring series
    val manualBoost: Double = 0.0,
    val nonPostponable: Boolean = false,
    val priorityScore: Double = 0.0,
    val status: String = "pending",           // pending|in_progress|completed|archived
    val postponedReason: String? = null,
    val createdAt: Long,                      // epoch ms
    val updatedAt: Long,                      // epoch ms
    val meta: String? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncVersion: Long = 1L
)

data class Session(
    val sessionId: String,
    val taskId: String?,
    val startAt: Long,
    val endAt: Long?,
    val mode: String?,
    val result: String?,
    val feeling: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncVersion: Long = 1L
)

enum class PriorityBand(val label: String, val minScore: Double) {
    URGENT("Urgente", 70.0),
    HIGH("Alta", 40.0),
    NORMAL("Normal", 20.0),
    LOW("Baja", 0.0);

    companion object {
        fun fromScore(score: Double): PriorityBand = when {
            score >= 70.0 -> URGENT
            score >= 40.0 -> HIGH
            score < 20.0 -> LOW
            else -> NORMAL
        }
    }
}

