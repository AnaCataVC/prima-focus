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
        override fun getCompletedTasksWithSessions() = throw UnsupportedOperationException()
        override fun deleteCompletedTasks() {}
        override suspend fun getTopTaskNow(): TaskEntity? = tasks.values.firstOrNull { it.status == "pending" }
        override suspend fun getTopThreeTasksNow(): List<TaskEntity> = tasks.values.filter { it.status == "pending" }.take(3)
        override fun getTaskById(taskId: String): TaskEntity? = tasks[taskId]
        override fun insertTask(task: TaskEntity) { tasks[task.taskId] = task }
        override fun insertTasks(tasks: List<TaskEntity>) { tasks.forEach { insertTask(it) } }
        override fun updateTask(task: TaskEntity) {
            tasks[task.taskId] = task
            lastCompletedTask = task
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
        override fun getSessionsForTask(taskId: String): Flow<List<SessionEntity>> = flowOf(sessions.filter { it.taskId == taskId })
        override fun insertSession(session: SessionEntity) { sessions.add(session) }
        override fun insertSessions(sessions: List<SessionEntity>) { this.sessions.addAll(sessions) }
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
}
