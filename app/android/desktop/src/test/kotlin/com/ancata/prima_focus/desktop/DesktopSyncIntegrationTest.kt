package com.ancata.prima_focus.desktop

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

        val importedTasks = secondDbManager.getPendingActiveTasks()
        assertEquals(1, importedTasks.size)
        assertEquals("Backup Task", importedTasks[0].title)

        secondDbFile.delete()
    }
}
