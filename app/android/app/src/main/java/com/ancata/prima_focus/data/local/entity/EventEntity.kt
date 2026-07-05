package com.ancata.prima_focus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val eventId: String,
    val taskId: String?,
    val type: String?,
    val scheduledAt: Long?,
    val attempt: Int = 0,
    val action: String?,
    val status: String?,
    val createdAt: Long,
    val updatedAt: Long
)
