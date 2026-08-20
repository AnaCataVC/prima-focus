package com.ancata.prima_focus.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSessions(
    @Embedded
    val task: TaskEntity,
    @Relation(
        parentColumn = "taskId",
        entityColumn = "taskId"
    )
    val sessions: List<SessionEntity>
)
