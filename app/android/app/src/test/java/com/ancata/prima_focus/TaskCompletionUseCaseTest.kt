package com.ancata.prima_focus

import com.ancata.prima_focus.data.local.dao.SessionDao
import com.ancata.prima_focus.data.local.dao.TaskDao
import com.ancata.prima_focus.data.local.entity.SessionEntity
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.domain.TaskCompletionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCompletionUseCaseTest {

    private class FakeTaskDao : TaskDao {
        val tasks = mutableMapOf<String, TaskEntity>()
        var lastCompletedTask: TaskEntity? = null
        var spawnedNextTask: TaskEntity? = null

        override fun getPendingTasksOrderedByPriority() = throw UnsupportedOperationException()
        override fun getAllTasks(): List<TaskEntity> = tasks.values.toList()
        override fun getActiveTasks(): List<TaskEntity> = tasks.values.filter { !it.isDeleted }
        override fun getCompletedTasksWithSessions() = throw UnsupportedOperationException()
        override fun deleteCompletedTasks() {}
        override suspend fun getTopTaskNow(): TaskEntity? = tasks.values.firstOrNull { it.status == "pending" && !it.isDeleted }
        override suspend fun getTopThreeTasksNow(): List<TaskEntity> = tasks.values.filter { it.status == "pending" && !it.isDeleted }.take(3)
        override fun getTaskById(taskId: String): TaskEntity? = tasks[taskId]
        override fun insertTask(task: TaskEntity) { tasks[task.taskId] = task }
        override fun insertTasks(tasks: List<TaskEntity>) { tasks.forEach { insertTask(it) } }
        override fun updateTask(task: TaskEntity) {
            tasks[task.taskId] = task
            lastCompletedTask = task
        }
        override fun softDeleteTask(taskId: String, deletedAt: Long, updatedAt: Long) {
            tasks[taskId]?.let { tasks[taskId] = it.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt) }
        }
        override fun softDeleteTasksByGroupId(groupId: String, deletedAt: Long, updatedAt: Long) {
            tasks.values.filter { it.recurrenceGroupId == groupId }.forEach {
                tasks[it.taskId] = it.copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt)
            }
        }
        override fun purgeOldTombstones(cutoffTimestamp: Long) {
            tasks.entries.removeIf { it.value.isDeleted && (it.value.deletedAt ?: 0L) < cutoffTimestamp }
        }
        override fun deleteTask(taskId: String) { tasks.remove(taskId) }
        override fun deleteTasksByGroupId(groupId: String) {}
        override fun clearAllTasks() { tasks.clear() }
        override fun completeAndSpawnNext(completed: TaskEntity, next: TaskEntity) {
            updateTask(completed)
            insertTask(next)
            spawnedNextTask = next
        }
        override fun restoreTasksAtomic(tasks: List<TaskEntity>, clearExisting: Boolean) {}
        override fun getCompletedRecurringTasks(): List<TaskEntity> = emptyList()
        override fun hasPendingInGroup(groupId: String): Boolean = false
    }

    private class FakeSessionDao : SessionDao {
        val sessions = mutableListOf<SessionEntity>()
        override fun getAllSessions(): List<SessionEntity> = sessions
        override fun getActiveSessions(): List<SessionEntity> = sessions.filter { !it.isDeleted }
        override fun getSessionsForTask(taskId: String): Flow<List<SessionEntity>> = flowOf(sessions.filter { it.taskId == taskId && !it.isDeleted })
        override fun insertSession(session: SessionEntity) { sessions.add(session) }
        override fun insertSessions(sessions: List<SessionEntity>) { this.sessions.addAll(sessions) }
        override fun softDeleteSession(sessionId: String, deletedAt: Long, updatedAt: Long) {
            val idx = sessions.indexOfFirst { it.sessionId == sessionId }
            if (idx != -1) sessions[idx] = sessions[idx].copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt)
        }
        override fun softDeleteSessionsForTask(taskId: String, deletedAt: Long, updatedAt: Long) {
            sessions.indices.filter { sessions[it].taskId == taskId }.forEach {
                sessions[it] = sessions[it].copy(isDeleted = true, deletedAt = deletedAt, updatedAt = updatedAt)
            }
        }
        override fun purgeOldTombstones(cutoffTimestamp: Long) {
            sessions.removeIf { it.isDeleted && (it.deletedAt ?: 0L) < cutoffTimestamp }
        }
        override fun deleteSessionsForTask(taskId: String) {}
        override fun deleteOrphanedSessions() {}
        override fun clearAllSessions() { sessions.clear() }
    }

    @Test
    fun `execute marks task as completed and logs session`() = runBlocking {
        val taskDao = FakeTaskDao()
        val sessionDao = FakeSessionDao()
        val useCase = TaskCompletionUseCase(taskDao, sessionDao)

        val task = TaskEntity(
            taskId = "task_1",
            title = "Test Task",
            description = null,
            category = "trabajo",
            subcategory = null,
            categoryWeight = 2.0,
            date = "2026-08-21",
            time = null,
            estimatedMinutes = 30,
            isProject = false,
            status = "pending",
            createdAt = 1000L,
            updatedAt = 1000L,
            meta = null,
            postponedReason = null,
            recurrence = null,
            recurrenceGroupId = null
        )
        taskDao.insertTask(task)

        val result = useCase.execute("task_1", feeling = 4, result = "completada con éxito")

        assertTrue(result)
        assertEquals("completed", taskDao.getTaskById("task_1")?.status)
        assertEquals(1, sessionDao.sessions.size)
        assertEquals(4, sessionDao.sessions.first().feeling)
        assertEquals("completada con éxito", sessionDao.sessions.first().result)
    }

    @Test
    fun `execute on recurring task spawns next occurrence`() = runBlocking {
        val taskDao = FakeTaskDao()
        val sessionDao = FakeSessionDao()
        val useCase = TaskCompletionUseCase(taskDao, sessionDao)

        val task = TaskEntity(
            taskId = "rec_task_1",
            title = "Daily Standup",
            description = null,
            category = "trabajo",
            subcategory = null,
            categoryWeight = 2.0,
            date = "2026-08-20",
            time = null,
            estimatedMinutes = 15,
            isProject = false,
            status = "pending",
            createdAt = 1000L,
            updatedAt = 1000L,
            meta = null,
            postponedReason = null,
            recurrence = "DAILY",
            recurrenceGroupId = "rec_task_1"
        )
        taskDao.insertTask(task)

        val result = useCase.execute("rec_task_1", feeling = 3, result = "Completada desde widget")

        assertTrue(result)
        assertEquals("completed", taskDao.getTaskById("rec_task_1")?.status)
        assertNotNull(taskDao.spawnedNextTask)
        assertEquals("pending", taskDao.spawnedNextTask?.status)
        assertEquals("Daily Standup", taskDao.spawnedNextTask?.title)
        assertEquals("2026-08-21", taskDao.spawnedNextTask?.date)
    }

    @Test
    fun `execute returns false if task already completed (idempotency)`() = runBlocking {
        val taskDao = FakeTaskDao()
        val sessionDao = FakeSessionDao()
        val useCase = TaskCompletionUseCase(taskDao, sessionDao)

        val task = TaskEntity(
            taskId = "task_already_done",
            title = "Done Task",
            description = null,
            category = "salud",
            subcategory = null,
            categoryWeight = 2.0,
            date = null,
            time = null,
            estimatedMinutes = null,
            isProject = false,
            status = "completed",
            createdAt = 1000L,
            updatedAt = 1000L,
            meta = null,
            postponedReason = null,
            recurrence = null,
            recurrenceGroupId = null
        )
        taskDao.insertTask(task)

        val result = useCase.execute("task_already_done")
        assertFalse(result)
        assertEquals(0, sessionDao.sessions.size)
    }

    @Test
    fun `execute logs feeling ratings 1, 3, 5 accurately for feedback review`() = runBlocking {
        val taskDao = FakeTaskDao()
        val sessionDao = FakeSessionDao()
        val useCase = TaskCompletionUseCase(taskDao, sessionDao)

        val task1 = TaskEntity(
            taskId = "task_bad",
            title = "Hard Task",
            description = null,
            category = "trabajo",
            subcategory = null,
            categoryWeight = 2.0,
            date = "2026-08-25",
            time = null,
            estimatedMinutes = 20,
            isProject = false,
            status = "pending",
            createdAt = 1000L,
            updatedAt = 1000L,
            meta = null,
            postponedReason = null,
            recurrence = null,
            recurrenceGroupId = null
        )
        taskDao.insertTask(task1)
        useCase.execute("task_bad", feeling = 1, result = "completed")
        assertEquals(1, sessionDao.sessions.last().feeling)

        val task2 = task1.copy(taskId = "task_good")
        taskDao.insertTask(task2)
        useCase.execute("task_good", feeling = 5, result = "completed")
        assertEquals(5, sessionDao.sessions.last().feeling)
    }
}
