package com.ancata.prima_focus

import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.core.sync.MergeAction
import com.ancata.prima_focus.core.sync.SyncMergeEngine
import com.ancata.prima_focus.core.sync.SyncSettings
import com.ancata.prima_focus.data.mapper.toDomain
import com.ancata.prima_focus.sync.SyncDataPayload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class P2PSyncTest {

    private val gson = Gson()

    @Test
    fun syncDataPayload_serializesAndDeserializes_tasksAndSessions_withTombstoneAndVersion() {
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
            meta = null,
            isDeleted = true,
            deletedAt = now,
            syncVersion = 3L
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
            updatedAt = now,
            isDeleted = true,
            deletedAt = now,
            syncVersion = 2L
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
        assertTrue(json.contains("\"isDeleted\":true"))
        assertTrue(json.contains("\"syncVersion\":3"))

        val deserialized = gson.fromJson(json, SyncDataPayload::class.java)
        assertEquals(1, deserialized.version)
        assertEquals(1, deserialized.tasks.size)
        assertEquals("task-sync-1", deserialized.tasks[0].taskId)
        assertTrue(deserialized.tasks[0].isDeleted)
        assertEquals(3L, deserialized.tasks[0].syncVersion)
        assertEquals(1, deserialized.sessions.size)
        assertTrue(deserialized.sessions[0].isDeleted)
        assertEquals(2L, deserialized.sessions[0].syncVersion)
    }

    @Test
    fun syncMerge_versionAwareConflictResolution_prefersHigherVersion() {
        val now = System.currentTimeMillis()
        val localTask = TaskEntity(
            taskId = "task-conflict-1",
            title = "Local Old Title",
            description = null,
            category = "trabajo",
            subcategory = null,
            categoryWeight = 2.0,
            date = null,
            time = null,
            createdAt = now - 50000,
            updatedAt = now + 100000, // Even if clock was ahead!
            meta = null,
            syncVersion = 1L
        )

        val receivedTask = TaskEntity(
            taskId = "task-conflict-1",
            title = "Remote New Title (Version 2)",
            description = null,
            category = "trabajo",
            subcategory = null,
            categoryWeight = 2.0,
            date = null,
            time = null,
            createdAt = now - 50000,
            updatedAt = now, // Clock was behind, but version is higher
            meta = null,
            syncVersion = 2L
        )

        // Version 2 should beat Version 1 regardless of clock drift
        val action = SyncMergeEngine.resolveTaskConflict(localTask.toDomain(), receivedTask.toDomain())
        assertTrue(action is MergeAction.Update)
        assertEquals("Remote New Title (Version 2)", (action as MergeAction.Update).item.title)
    }

    @Test
    fun syncMerge_tombstoneSoftDelete_propagatesCorrectly() {
        val now = System.currentTimeMillis()
        val localTask = TaskEntity(
            taskId = "task-tombstone-1",
            title = "Task To Be Deleted",
            description = null,
            category = "trabajo",
            subcategory = null,
            categoryWeight = 2.0,
            date = null,
            time = null,
            createdAt = now - 50000,
            updatedAt = now - 10000,
            meta = null,
            isDeleted = false,
            syncVersion = 1L
        )

        val receivedDeletedTask = localTask.copy(
            isDeleted = true,
            deletedAt = now,
            updatedAt = now,
            syncVersion = 2L
        )

        val action = SyncMergeEngine.resolveTaskConflict(localTask.toDomain(), receivedDeletedTask.toDomain())
        assertTrue(action is MergeAction.Update)
        assertTrue((action as MergeAction.Update).item.isDeleted)
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
        assertFalse(parsedTasks[0].isDeleted) // Default boolean
        assertEquals(1L, parsedTasks[0].syncVersion) // Default syncVersion
    }

    @Test
    fun syncPayloadV2_roundTripsDeviceIdAndSettings() {
        val payload = SyncDataPayload(
            deviceId = "device-a",
            settings = SyncSettings(mapOf("casa" to "🧹"), skipMissedOccurrences = false, updatedAt = 42L)
        )

        val parsed = gson.fromJson(gson.toJson(payload), SyncDataPayload::class.java)

        assertEquals(SyncDataPayload.CURRENT_PAYLOAD_VERSION, parsed.version)
        assertEquals("device-a", parsed.deviceId)
        assertEquals(payload.settings, parsed.settings)
    }

    @Test
    fun syncPayloadV1_withoutSettingsStillParses() {
        val v1Json = """{"version":1,"tasks":[],"sessions":[]}"""

        val parsed = gson.fromJson(v1Json, SyncDataPayload::class.java)

        assertEquals(1, parsed.version)
        assertNull(parsed.deviceId)
        assertNull(parsed.settings)
    }
}
