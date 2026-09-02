package com.ancata.prima_focus.data.mapper

import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.data.local.entity.TaskEntity

fun TaskEntity.toDomain(): Task = Task(
    taskId = taskId,
    title = title,
    description = description,
    category = category,
    subcategory = subcategory,
    categoryWeight = categoryWeight,
    date = date,
    time = time,
    hasTime = hasTime,
    timeUrgency = timeUrgency,
    estimatedMinutes = estimatedMinutes,
    isProject = isProject,
    recurrence = recurrence,
    recurrenceGroupId = recurrenceGroupId,
    manualBoost = manualBoost,
    nonPostponable = nonPostponable,
    priorityScore = priorityScore,
    status = status,
    postponedReason = postponedReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    meta = meta,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncVersion = syncVersion
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    taskId = taskId,
    title = title,
    description = description,
    category = category,
    subcategory = subcategory,
    categoryWeight = categoryWeight,
    date = date,
    time = time,
    hasTime = hasTime,
    timeUrgency = timeUrgency,
    estimatedMinutes = estimatedMinutes,
    isProject = isProject,
    recurrence = recurrence,
    recurrenceGroupId = recurrenceGroupId,
    manualBoost = manualBoost,
    nonPostponable = nonPostponable,
    priorityScore = priorityScore,
    status = status,
    postponedReason = postponedReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    meta = meta,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncVersion = syncVersion
)

fun SessionEntity.toDomain(): Session = Session(
    sessionId = sessionId,
    taskId = taskId,
    startAt = startAt,
    endAt = endAt,
    mode = mode,
    result = result,
    feeling = feeling,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncVersion = syncVersion
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    sessionId = sessionId,
    taskId = taskId,
    startAt = startAt,
    endAt = endAt,
    mode = mode,
    result = result,
    feeling = feeling,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    syncVersion = syncVersion
)
