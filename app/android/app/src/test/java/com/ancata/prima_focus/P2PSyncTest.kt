package com.ancata.prima_focus

import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.sync.SyncDataPayload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class P2PSyncTest {

    private val gson = Gson()

    @Test
    fun syncDataPayload_serializesAndDeserializes_tasksAndSessions() {
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            taskId = "task-sync-1",
            title = "Sync Test Task",
            description = "Notes for cross-device sync",
            category = "trabajo",
            subcategory = "entrega",
            categoryWeight = 3.5,
            date = "Hoy",
            time = null,
            estimatedMinutes = 45,
            recurrence = null,
            recurrenceGroupId = null,
            status = "completed",
            postponedReason = null,
            createdAt = now - 10000,
            updatedAt = now,
            meta = null
        )

        val session = SessionEntity(
            sessionId = "session-sync-1",
            taskId = "task-sync-1",
            startAt = now - (45 * 60 * 1000L),
            endAt = now,
            mode = "focus",
            result = "completed",
            feeling = 5,
            createdAt = now,
            updatedAt = now
        )

        val payload = SyncDataPayload(
            version = 1,
            tasks = listOf(task),
            sessions = listOf(session)
        )

        val json = gson.toJson(payload)
        assertNotNull(json)
        assertTrue(json.contains("Sync Test Task"))
        assertTrue(json.contains("session-sync-1"))

        val deserialized = gson.fromJson(json, SyncDataPayload::class.java)
        assertEquals(1, deserialized.version)
        assertEquals(1, deserialized.tasks.size)
        assertEquals("task-sync-1", deserialized.tasks[0].taskId)
        assertEquals(45, deserialized.tasks[0].estimatedMinutes)
        assertEquals(1, deserialized.sessions.size)
        assertEquals("session-sync-1", deserialized.sessions[0].sessionId)
        assertEquals(5, deserialized.sessions[0].feeling)
    }

    @Test
    fun syncPayload_backwardCompatibility_legacyArrayParsing() {
        val now = System.currentTimeMillis()
        val legacyTasks = listOf(
            TaskEntity(
                taskId = "task-legacy-1",
                title = "Legacy Task",
                description = null,
                category = "casa",
                subcategory = null,
                categoryWeight = 2.0,
                date = null,
                time = null,
                estimatedMinutes = 15,
                recurrence = null,
                recurrenceGroupId = null,
                postponedReason = null,
                createdAt = now,
                updatedAt = now,
                meta = null
            )
        )

        val legacyJson = gson.toJson(legacyTasks)
        val trimmed = legacyJson.trim()
        assertTrue(trimmed.startsWith("["))

        val listType = object : TypeToken<List<TaskEntity>>() {}.type
        val parsedTasks: List<TaskEntity> = gson.fromJson(legacyJson, listType)

        assertEquals(1, parsedTasks.size)
        assertEquals("Legacy Task", parsedTasks[0].title)
    }
}
