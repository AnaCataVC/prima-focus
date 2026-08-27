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

    fun getAllTasks(): List<Task> {
        getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM tasks").use { stmt ->
                val rs = stmt.executeQuery()
                val list = mutableListOf<Task>()
                while (rs.next()) {
                    list.add(
                        Task(
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
                    )
                }
                return list
            }
        }
    }

    fun getPendingActiveTasks(): List<Task> {
        return getAllTasks().filter { it.status == "pending" && !it.isDeleted }
            .sortedWith(
                compareByDescending<Task> { it.priorityScore }
                    .thenByDescending { it.hasTime }
                    .thenBy { it.date ?: "9999-99-99" }
                    .thenBy { it.createdAt }
            )
    }

    fun insertOrUpdateTask(task: Task) {
        val sql = """
            INSERT OR REPLACE INTO tasks (
                taskId, title, description, category, subcategory, categoryWeight,
                date, time, hasTime, timeUrgency, estimatedMinutes, isProject,
                recurrence, recurrenceGroupId, manualBoost, nonPostponable,
                priorityScore, status, postponedReason, createdAt, updatedAt,
                meta, isDeleted, deletedAt, syncVersion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        val estMin = task.estimatedMinutes
        val delAt = task.deletedAt

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
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
                    list.add(
                        Session(
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
                    )
                }
                return list
            }
        }
    }

    fun insertOrUpdateSession(session: Session) {
        val sql = """
            INSERT OR REPLACE INTO sessions (
                sessionId, taskId, startAt, endAt, mode, result, feeling,
                createdAt, updatedAt, isDeleted, deletedAt, syncVersion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        val endAtVal = session.endAt
        val feelVal = session.feeling
        val delAtVal = session.deletedAt

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
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
                stmt.executeUpdate()
            }
        }
    }

    fun mergeSyncPayload(remoteTasks: List<Task>, remoteSessions: List<Session>): Int {
        val localTasks = getAllTasks().associateBy { it.taskId }
        val localSessions = getAllSessions().associateBy { it.sessionId }
        var changesCount = 0

        remoteTasks.forEach { remoteTask ->
            val local = localTasks[remoteTask.taskId]
            when (val action = SyncMergeEngine.resolveTaskConflict(local, remoteTask)) {
                is MergeAction.Insert -> {
                    insertOrUpdateTask(action.item)
                    changesCount++
                }
                is MergeAction.Update -> {
                    insertOrUpdateTask(action.item)
                    changesCount++
                }
                is MergeAction.KeepLocal -> {}
            }
        }

        remoteSessions.forEach { remoteSession ->
            val local = localSessions[remoteSession.sessionId]
            when (val action = SyncMergeEngine.resolveSessionConflict(local, remoteSession)) {
                is MergeAction.Insert -> {
                    insertOrUpdateSession(action.item)
                    changesCount++
                }
                is MergeAction.Update -> {
                    insertOrUpdateSession(action.item)
                    changesCount++
                }
                is MergeAction.KeepLocal -> {}
            }
        }

        return changesCount
    }
}
