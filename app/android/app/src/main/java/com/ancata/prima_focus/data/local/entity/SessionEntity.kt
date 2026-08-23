package com.ancata.prima_focus.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

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
    indices = [Index("taskId")]
)
data class SessionEntity(
    @PrimaryKey
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
