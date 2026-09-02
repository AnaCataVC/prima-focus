package com.ancata.prima_focus.desktop.db

import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.core.sync.MergeAction
import com.ancata.prima_focus.core.sync.SyncMergeEngine
import java.io.File
import java.sql.Connection
import java.sql.DriverManager


class DesktopDatabaseManager(
    dbFilePath: String? = null
) {
    private val dbPath: String = dbFilePath ?: run {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "PrimaFocus")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "primafocus_desktop.db").absolutePath
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }

    init {
        initSchema()
    }

    private fun initSchema() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // P1: Enable WAL mode to prevent SQLITE_BUSY between HTTP sync server and EDT reader
                stmt.execute("PRAGMA journal_mode=WAL;")
                stmt.execute("PRAGMA busy_timeout=5000;")
                stmt.execute("PRAGMA synchronous=NORMAL;")

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        taskId TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        category TEXT NOT NULL,
                        subcategory TEXT,
                        categoryWeight REAL NOT NULL,
                        date TEXT,
                        time TEXT,
                        hasTime INTEGER NOT NULL DEFAULT 0,
                        timeUrgency REAL NOT NULL DEFAULT 0.0,
                        estimatedMinutes INTEGER,
                        isProject INTEGER NOT NULL DEFAULT 0,
                        recurrence TEXT,
                        recurrenceGroupId TEXT,
                        manualBoost REAL NOT NULL DEFAULT 0.0,
                        nonPostponable INTEGER NOT NULL DEFAULT 0,
                        priorityScore REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'pending',
                        postponedReason TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        meta TEXT,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER DEFAULT NULL,
                        syncVersion INTEGER NOT NULL DEFAULT 1
                    );
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        sessionId TEXT PRIMARY KEY,
                        taskId TEXT,
                        startAt INTEGER NOT NULL,
                        endAt INTEGER,
                        mode TEXT,
                        result TEXT,
                        feeling INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER DEFAULT NULL,
                        syncVersion INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY (taskId) REFERENCES tasks(taskId) ON DELETE SET NULL
                    );
                """.trimIndent())

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_status_priority ON tasks(status, priorityScore DESC);")
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_tasks_updatedAt ON tasks(updatedAt);")
            }
        }
    }

    companion object {
        private val TASK_INSERT_SQL = """
            INSERT OR REPLACE INTO tasks (
                taskId, title, description, category, subcategory, categoryWeight,
                date, time, hasTime, timeUrgency, estimatedMinutes, isProject,
                recurrence, recurrenceGroupId, manualBoost, nonPostponable,
                priorityScore, status, postponedReason, createdAt, updatedAt,
                meta, isDeleted, deletedAt, syncVersion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        private val SESSION_INSERT_SQL = """
            INSERT OR REPLACE INTO sessions (
                sessionId, taskId, startAt, endAt, mode, result, feeling,
                createdAt, updatedAt, isDeleted, deletedAt, syncVersion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()
    }

    private fun mapResultSetToTask(rs: java.sql.ResultSet): Task {
        return Task(
            taskId = rs.getString("taskId"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            category = rs.getString("category"),
            subcategory = rs.getString("subcategory"),
            categoryWeight = rs.getDouble("categoryWeight"),
            date = rs.getString("date"),
            time = rs.getString("time"),
            hasTime = rs.getInt("hasTime") == 1,
            timeUrgency = rs.getDouble("timeUrgency"),
            estimatedMinutes = rs.getObject("estimatedMinutes") as? Int,
            isProject = rs.getInt("isProject") == 1,
            recurrence = rs.getString("recurrence"),
            recurrenceGroupId = rs.getString("recurrenceGroupId"),
            manualBoost = rs.getDouble("manualBoost"),
            nonPostponable = rs.getInt("nonPostponable") == 1,
            priorityScore = rs.getDouble("priorityScore"),
            status = rs.getString("status"),
            postponedReason = rs.getString("postponedReason"),
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            meta = rs.getString("meta"),
            isDeleted = rs.getInt("isDeleted") == 1,
            deletedAt = rs.getObject("deletedAt") as? Long,
            syncVersion = rs.getLong("syncVersion")
        )
    }

    private fun mapResultSetToSession(rs: java.sql.ResultSet): Session {
        return Session(
            sessionId = rs.getString("sessionId"),
            taskId = rs.getString("taskId"),
            startAt = rs.getLong("startAt"),
            endAt = rs.getObject("endAt") as? Long,
            mode = rs.getString("mode"),
            result = rs.getString("result"),
            feeling = rs.getObject("feeling") as? Int,
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            isDeleted = rs.getInt("isDeleted") == 1,
            deletedAt = rs.getObject("deletedAt") as? Long,
            syncVersion = rs.getLong("syncVersion")
        )
    }

    private fun bindTaskStatement(stmt: java.sql.PreparedStatement, task: Task) {
        val estMin = task.estimatedMinutes
        val delAt = task.deletedAt
        stmt.setString(1, task.taskId)
        stmt.setString(2, task.title)
        stmt.setString(3, task.description)
        stmt.setString(4, task.category)
        stmt.setString(5, task.subcategory)
        stmt.setDouble(6, task.categoryWeight)
        stmt.setString(7, task.date)
        stmt.setString(8, task.time)
        stmt.setInt(9, if (task.hasTime) 1 else 0)
        stmt.setDouble(10, task.timeUrgency)
        if (estMin != null) stmt.setInt(11, estMin) else stmt.setNull(11, java.sql.Types.INTEGER)
        stmt.setInt(12, if (task.isProject) 1 else 0)
        stmt.setString(13, task.recurrence)
        stmt.setString(14, task.recurrenceGroupId)
        stmt.setDouble(15, task.manualBoost)
        stmt.setInt(16, if (task.nonPostponable) 1 else 0)
        stmt.setDouble(17, task.priorityScore)
        stmt.setString(18, task.status)
        stmt.setString(19, task.postponedReason)
        stmt.setLong(20, task.createdAt)
        stmt.setLong(21, task.updatedAt)
        stmt.setString(22, task.meta)
        stmt.setInt(23, if (task.isDeleted) 1 else 0)
        if (delAt != null) stmt.setLong(24, delAt) else stmt.setNull(24, java.sql.Types.BIGINT)
        stmt.setLong(25, task.syncVersion)
    }

    private fun bindSessionStatement(stmt: java.sql.PreparedStatement, session: Session) {
        val endAtVal = session.endAt
        val feelVal = session.feeling
        val delAtVal = session.deletedAt
        stmt.setString(1, session.sessionId)
        stmt.setString(2, session.taskId)
        stmt.setLong(3, session.startAt)
        if (endAtVal != null) stmt.setLong(4, endAtVal) else stmt.setNull(4, java.sql.Types.BIGINT)
        stmt.setString(5, session.mode)
        stmt.setString(6, session.result)
        if (feelVal != null) stmt.setInt(7, feelVal) else stmt.setNull(7, java.sql.Types.INTEGER)
        stmt.setLong(8, session.createdAt)
        stmt.setLong(9, session.updatedAt)
        stmt.setInt(10, if (session.isDeleted) 1 else 0)
        if (delAtVal != null) stmt.setLong(11, delAtVal) else stmt.setNull(11, java.sql.Types.BIGINT)
        stmt.setLong(12, session.syncVersion)
    }

    fun getAllTasks(): List<Task> {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM tasks").use { stmt ->
                val rs = stmt.executeQuery()
                val list = mutableListOf<Task>()
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs))
                }
                return list
            }
        }
    }

    fun getPendingActiveTasks(): List<Task> {
        val sql = """
            SELECT * FROM tasks 
            WHERE status = 'pending' AND isDeleted = 0 
            ORDER BY priorityScore DESC, hasTime DESC, 
                     CASE WHEN date IS NULL THEN '9999-99-99' ELSE date END ASC, 
                     createdAt ASC
        """.trimIndent()
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val list = mutableListOf<Task>()
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs))
                }
                return list
            }
        }
    }

    fun insertOrUpdateTask(task: Task) {
        getConnection().use { conn ->
            conn.prepareStatement(TASK_INSERT_SQL).use { stmt ->
                bindTaskStatement(stmt, task)
                stmt.executeUpdate()
            }
        }
    }

    fun getAllSessions(): List<Session> {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM sessions").use { stmt ->
                val rs = stmt.executeQuery()
                val list = mutableListOf<Session>()
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs))
                }
                return list
            }
        }
    }

    fun insertOrUpdateSession(session: Session) {
        getConnection().use { conn ->
            conn.prepareStatement(SESSION_INSERT_SQL).use { stmt ->
                bindSessionStatement(stmt, session)
                stmt.executeUpdate()
            }
        }
    }

    fun mergeSyncPayload(remoteTasks: List<Task>, remoteSessions: List<Session>): Int {
        val localTasks = getAllTasks().associateBy { it.taskId }
        val localSessions = getAllSessions().associateBy { it.sessionId }
        
        val tasksToPersist = mutableListOf<Task>()
        val sessionsToPersist = mutableListOf<Session>()

        remoteTasks.forEach { remoteTask ->
            val local = localTasks[remoteTask.taskId]
            when (val action = SyncMergeEngine.resolveTaskConflict(local, remoteTask)) {
                is MergeAction.Insert -> tasksToPersist.add(action.item)
                is MergeAction.Update -> tasksToPersist.add(action.item)
                is MergeAction.KeepLocal -> {}
            }
        }

        remoteSessions.forEach { remoteSession ->
            val local = localSessions[remoteSession.sessionId]
            when (val action = SyncMergeEngine.resolveSessionConflict(local, remoteSession)) {
                is MergeAction.Insert -> sessionsToPersist.add(action.item)
                is MergeAction.Update -> sessionsToPersist.add(action.item)
                is MergeAction.KeepLocal -> {}
            }
        }

        if (tasksToPersist.isEmpty() && sessionsToPersist.isEmpty()) {
            return 0
        }

        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                if (tasksToPersist.isNotEmpty()) {
                    conn.prepareStatement(TASK_INSERT_SQL).use { stmt ->
                        for (task in tasksToPersist) {
                            bindTaskStatement(stmt, task)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                }

                if (sessionsToPersist.isNotEmpty()) {
                    conn.prepareStatement(SESSION_INSERT_SQL).use { stmt ->
                        for (session in sessionsToPersist) {
                            bindSessionStatement(stmt, session)
                            stmt.addBatch()
                        }
                        stmt.executeBatch()
                    }
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }

        return tasksToPersist.size + sessionsToPersist.size
    }
}
