package com.ancata.prima_focus

import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.mapper.toDomain
import com.ancata.prima_focus.data.mapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMapperTest {

    @Test
    fun taskEntity_toDomain_andBack_preservesAllTwentyFiveFields() {
        val originalEntity = TaskEntity(
            taskId = "task-mapper-1",
            title = "Complete architecture audit",
            description = "Audit description detailing invariants",
            category = "trabajo",
            subcategory = "arquitectura",
            categoryWeight = 3.0,
            date = "2026-09-02",
            time = "14:30",
            hasTime = true,
            timeUrgency = 12.5,
            estimatedMinutes = 45,
            isProject = true,
            recurrence = "DAILY",
            recurrenceGroupId = "rec-group-1",
            manualBoost = 10.0,
            nonPostponable = true,
            priorityScore = 88.5,
            status = "pending",
            postponedReason = "Waiting for review",
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L,
            meta = "{\"tag\":\"kmp\"}",
            isDeleted = false,
            deletedAt = null,
            syncVersion = 4L
        )

        val domainModel = originalEntity.toDomain()

        assertEquals(originalEntity.taskId, domainModel.taskId)
        assertEquals(originalEntity.title, domainModel.title)
        assertEquals(originalEntity.description, domainModel.description)
        assertEquals(originalEntity.category, domainModel.category)
        assertEquals(originalEntity.subcategory, domainModel.subcategory)
        assertEquals(originalEntity.categoryWeight, domainModel.categoryWeight, 0.001)
        assertEquals(originalEntity.date, domainModel.date)
        assertEquals(originalEntity.time, domainModel.time)
        assertEquals(originalEntity.hasTime, domainModel.hasTime)
        assertEquals(originalEntity.timeUrgency, domainModel.timeUrgency, 0.001)
        assertEquals(originalEntity.estimatedMinutes, domainModel.estimatedMinutes)
        assertEquals(originalEntity.isProject, domainModel.isProject)
        assertEquals(originalEntity.recurrence, domainModel.recurrence)
        assertEquals(originalEntity.recurrenceGroupId, domainModel.recurrenceGroupId)
        assertEquals(originalEntity.manualBoost, domainModel.manualBoost, 0.001)
        assertEquals(originalEntity.nonPostponable, domainModel.nonPostponable)
        assertEquals(originalEntity.priorityScore, domainModel.priorityScore, 0.001)
        assertEquals(originalEntity.status, domainModel.status)
        assertEquals(originalEntity.postponedReason, domainModel.postponedReason)
        assertEquals(originalEntity.createdAt, domainModel.createdAt)
        assertEquals(originalEntity.updatedAt, domainModel.updatedAt)
        assertEquals(originalEntity.meta, domainModel.meta)
        assertEquals(originalEntity.isDeleted, domainModel.isDeleted)
        assertEquals(originalEntity.deletedAt, domainModel.deletedAt)
        assertEquals(originalEntity.syncVersion, domainModel.syncVersion)

        val roundTripEntity = domainModel.toEntity()
        assertEquals(originalEntity, roundTripEntity)
    }

    @Test
    fun sessionEntity_toDomain_andBack_preservesAllTwelveFields() {
        val originalSession = SessionEntity(
            sessionId = "session-123",
            taskId = "task-mapper-1",
            startAt = 1700000000000L,
            endAt = 1700001500000L,
            mode = "POMODORO",
            result = "completed",
            feeling = 5,
            createdAt = 1700000000000L,
            updatedAt = 1700001500000L,
            isDeleted = false,
            deletedAt = null,
            syncVersion = 2L
        )

        val domainSession = originalSession.toDomain()
        assertEquals(originalSession.sessionId, domainSession.sessionId)
        assertEquals(originalSession.taskId, domainSession.taskId)
        assertEquals(originalSession.startAt, domainSession.startAt)
        assertEquals(originalSession.endAt, domainSession.endAt)
        assertEquals(originalSession.mode, domainSession.mode)
        assertEquals(originalSession.result, domainSession.result)
        assertEquals(originalSession.feeling, domainSession.feeling)
        assertEquals(originalSession.createdAt, domainSession.createdAt)
        assertEquals(originalSession.updatedAt, domainSession.updatedAt)
        assertEquals(originalSession.isDeleted, domainSession.isDeleted)
        assertEquals(originalSession.deletedAt, domainSession.deletedAt)
        assertEquals(originalSession.syncVersion, domainSession.syncVersion)

        val roundTrip = domainSession.toEntity()
        assertEquals(originalSession, roundTrip)
    }

    @Test
    fun mapper_handlesNullableFieldsGracefully() {
        val minimalTask = Task(
            taskId = "min-1",
            title = "Minimal Task",
            category = "casa",
            categoryWeight = 1.0,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val entity = minimalTask.toEntity()
        assertNull(entity.description)
        assertNull(entity.subcategory)
        assertNull(entity.date)
        assertNull(entity.time)
        assertNull(entity.estimatedMinutes)
        assertNull(entity.recurrence)
        assertNull(entity.recurrenceGroupId)
        assertNull(entity.deletedAt)
        assertEquals(1L, entity.syncVersion)

        val backToDomain = entity.toDomain()
        assertEquals(minimalTask, backToDomain)
    }
}
