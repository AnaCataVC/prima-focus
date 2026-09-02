package com.ancata.prima_focus.desktop

import com.ancata.prima_focus.core.model.Session
import com.ancata.prima_focus.core.model.Task
import com.ancata.prima_focus.core.sync.LANAuthSecurity
import com.ancata.prima_focus.core.sync.PairingRequest
import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.ancata.prima_focus.desktop.sync.DesktopSyncServer
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class DesktopSyncIntegrationTest {

    private lateinit var tempDbFile: File
    private lateinit var dbManager: DesktopDatabaseManager
    private lateinit var syncServer: DesktopSyncServer
    private val testPort = 8799
    private val gson = Gson()

    @Before
    fun setUp() {
        tempDbFile = File.createTempFile("test_primafocus_desktop_", ".db")
        dbManager = DesktopDatabaseManager(tempDbFile.absolutePath)
        syncServer = DesktopSyncServer(dbManager, port = testPort)
        syncServer.start()
    }

    @After
    fun tearDown() {
        syncServer.stop()
        tempDbFile.delete()
    }

    @Test
    fun databaseManager_insertAndRetrieveTasks_orderedByPriority() {
        val now = System.currentTimeMillis()
        val task1 = Task(
            taskId = "t-low",
            title = "Low Priority",
            category = "casa",
            categoryWeight = 1.0,
            priorityScore = 10.0,
            createdAt = now,
            updatedAt = now
        )
        val task2 = Task(
            taskId = "t-high",
            title = "High Priority",
            category = "trabajo",
            categoryWeight = 4.0,
            priorityScore = 80.0,
            createdAt = now,
            updatedAt = now
        )

        dbManager.insertOrUpdateTask(task1)
        dbManager.insertOrUpdateTask(task2)

        val pending = dbManager.getPendingActiveTasks()
        assertEquals(2, pending.size)
        assertEquals("t-high", pending[0].taskId)
        assertEquals("t-low", pending[1].taskId)
    }

    @Test
    fun syncServer_pairingAndSecurityGate_verifiesPINandRejectsInvalidSignatures() {
        val pin = syncServer.currentPin
        val pinHash = LANAuthSecurity.hashPin(pin)

        // 1. Attempt Pairing with Valid PIN
        val pairUrl = URL("http://localhost:$testPort/api/pair")
        val pairConn = pairUrl.openConnection() as HttpURLConnection
        pairConn.requestMethod = "POST"
        pairConn.doOutput = true

        val pairReq = PairingRequest(
            deviceId = "test-mobile-id",
            deviceName = "Test Android Phone",
            pinHash = pinHash
        )
        pairConn.outputStream.use { it.write(gson.toJson(pairReq).toByteArray(StandardCharsets.UTF_8)) }

        assertEquals(200, pairConn.responseCode)
        val sessionKey = syncServer.sessionKey
        assertNotNull(sessionKey)

        // 2. Attempt Sync WITHOUT Signature (Must be rejected with 403)
        val syncUrl = URL("http://localhost:$testPort/api/sync")
        val unauthConn = syncUrl.openConnection() as HttpURLConnection
        unauthConn.requestMethod = "POST"
        unauthConn.doOutput = true
        unauthConn.outputStream.use { it.write("{}".toByteArray(StandardCharsets.UTF_8)) }
        assertEquals(403, unauthConn.responseCode)

        // 3. Attempt Sync WITH Valid HMAC-SHA256 Signature (Must succeed with 200)
        val now = System.currentTimeMillis()
        val mobileTask = Task(
            taskId = "mobile-t-1",
            title = "Synced from Mobile",
            category = "salud",
            categoryWeight = 4.0,
            priorityScore = 75.0,
            createdAt = now,
            updatedAt = now,
            syncVersion = 1L
        )

        val packet = com.ancata.prima_focus.core.sync.LANSyncPacket(
            deviceId = "test-mobile-id",
            deviceName = "Test Android Phone",
            tasks = listOf(mobileTask)
        )
        val packetJson = gson.toJson(packet)
        val signature = LANAuthSecurity.signPayload(packetJson, sessionKey!!)

        val authConn = syncUrl.openConnection() as HttpURLConnection
        authConn.requestMethod = "POST"
        authConn.setRequestProperty("X-Auth-Signature", signature)
        authConn.doOutput = true
        authConn.outputStream.use { it.write(packetJson.toByteArray(StandardCharsets.UTF_8)) }

        assertEquals(200, authConn.responseCode)
        val pendingAfterSync = dbManager.getPendingActiveTasks()
        assertEquals(1, pendingAfterSync.size)
        assertEquals("mobile-t-1", pendingAfterSync[0].taskId)
        assertEquals("Synced from Mobile", pendingAfterSync[0].title)
    }

    @Test
    fun fallbackBackup_exportAndImport_restoresDataCorrectly() {
        val now = System.currentTimeMillis()
        val originalTask = Task(
            taskId = "t-backup",
            title = "Backup Task",
            category = "estudio",
            categoryWeight = 3.0,
            priorityScore = 50.0,
            createdAt = now,
            updatedAt = now
        )
        dbManager.insertOrUpdateTask(originalTask)

        val json = syncServer.exportBackupJson()
        assertTrue(json.contains("Backup Task"))

        // Create new fresh DB and import
        val secondDbFile = File.createTempFile("test_second_desktop_", ".db")
        val secondDbManager = DesktopDatabaseManager(secondDbFile.absolutePath)
        val secondSyncServer = DesktopSyncServer(secondDbManager, port = 8798)

        val importedCount = secondSyncServer.importBackupJson(json)
        assertEquals(1, importedCount)

        secondDbFile.delete()
    }

    @Test
    fun lanSyncClient_executesFullSync_successfullyAndExchangesData() = kotlinx.coroutines.runBlocking {
        val client = com.ancata.prima_focus.core.sync.LanSyncClient()
        val now = System.currentTimeMillis()

        val mobileTask = Task(
            taskId = "client-task-1",
            title = "Task from LanSyncClient",
            category = "trabajo",
            categoryWeight = 3.0,
            priorityScore = 85.0,
            createdAt = now,
            updatedAt = now,
            syncVersion = 1L
        )

        val packet = com.ancata.prima_focus.core.sync.LANSyncPacket(
            deviceId = "test-client-device",
            deviceName = "Kotlin LanSyncClient",
            tasks = listOf(mobileTask)
        )

        val result = client.executeFullSync("localhost", testPort, syncServer.currentPin, packet)
        assertTrue(result.isSuccess)

        val remotePacket = result.getOrNull()
        assertNotNull(remotePacket)

        // Verify task was merged into server DB
        val serverTasks = dbManager.getPendingActiveTasks()
        assertTrue(serverTasks.any { it.taskId == "client-task-1" })
    }

    @Test
    fun syncServer_rateLimiting_blocksAfterFiveFailedAttempts() = kotlinx.coroutines.runBlocking {
        val client = com.ancata.prima_focus.core.sync.LanSyncClient()
        val dummyPacket = com.ancata.prima_focus.core.sync.LANSyncPacket(
            deviceId = "attacker",
            deviceName = "Attacker Device"
        )

        // Attempt 5 incorrect PINs
        for (i in 1..4) {
            val res = client.executeFullSync("localhost", testPort, "00000$i", dummyPacket)
            assertTrue(res.isFailure)
        }

        // 5th attempt triggers lockout
        val fifth = client.executeFullSync("localhost", testPort, "000005", dummyPacket)
        assertTrue(fifth.isFailure)

        // Next attempt must be rejected immediately with lockout / 429
        val lockedOut = client.executeFullSync("localhost", testPort, syncServer.currentPin, dummyPacket)
        assertTrue(lockedOut.isFailure)
    }

    @Test
    fun syncServer_ipResolution_returnsValidAddress() {
        val ip = syncServer.getLocalIpAddress()
        assertNotNull(ip)
        assertTrue(ip.isNotBlank())
        assertFalse(ip.contains("docker"))
        assertFalse(ip.contains("wsl"))
    }

    @Test
    fun getPendingActiveTasks_orderedBySqlIndex() {
        val now = System.currentTimeMillis()
        val lowTask = Task(
            taskId = "low_1",
            title = "Low Priority Task",
            category = "casa",
            categoryWeight = 1.0,
            priorityScore = 15.0,
            status = "pending",
            createdAt = now,
            updatedAt = now
        )
        val highTask = Task(
            taskId = "high_1",
            title = "High Priority Task",
            category = "trabajo",
            categoryWeight = 3.0,
            priorityScore = 85.0,
            status = "pending",
            createdAt = now + 100,
            updatedAt = now + 100
        )
        val deletedTask = Task(
            taskId = "del_1",
            title = "Deleted Task",
            category = "salud",
            categoryWeight = 3.0,
            priorityScore = 90.0,
            status = "pending",
            isDeleted = true,
            createdAt = now,
            updatedAt = now
        )
        val completedTask = Task(
            taskId = "comp_1",
            title = "Completed Task",
            category = "finanzas",
            categoryWeight = 3.0,
            priorityScore = 95.0,
            status = "completed",
            createdAt = now,
            updatedAt = now
        )

        dbManager.insertOrUpdateTask(lowTask)
        dbManager.insertOrUpdateTask(highTask)
        dbManager.insertOrUpdateTask(deletedTask)
        dbManager.insertOrUpdateTask(completedTask)

        val pending = dbManager.getPendingActiveTasks()
        assertTrue(pending.size >= 2)
        assertEquals("high_1", pending.first().taskId)
        assertFalse(pending.any { it.taskId == "del_1" })
        assertFalse(pending.any { it.taskId == "comp_1" })
    }

    @Test
    fun mergeSyncPayload_batchExecution_persistsTasksAndSessionsAtomically() {
        val now = System.currentTimeMillis()
        val batchTasks = listOf(
            Task(taskId = "b_task_1", title = "Batch 1", category = "trabajo", categoryWeight = 2.0, createdAt = now, updatedAt = now),
            Task(taskId = "b_task_2", title = "Batch 2", category = "estudio", categoryWeight = 2.0, createdAt = now, updatedAt = now)
        )
        val batchSessions = listOf(
            Session(sessionId = "b_sess_1", taskId = "b_task_1", startAt = now, endAt = now + 1500, mode = "POMODORO", result = "completed", feeling = 5, createdAt = now, updatedAt = now)
        )

        val changes = dbManager.mergeSyncPayload(batchTasks, batchSessions)
        assertEquals(3, changes)

        val retrievedTasks = dbManager.getAllTasks()
        assertTrue(retrievedTasks.any { it.taskId == "b_task_1" })
        assertTrue(retrievedTasks.any { it.taskId == "b_task_2" })

        val retrievedSessions = dbManager.getAllSessions()
        assertTrue(retrievedSessions.any { it.sessionId == "b_sess_1" })
    }
}
