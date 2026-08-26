package com.ancata.prima_focus.desktop.sync

import com.ancata.prima_focus.core.sync.LANAuthSecurity
import com.ancata.prima_focus.core.sync.LANSyncPacket
import com.ancata.prima_focus.core.sync.PairingRequest
import com.ancata.prima_focus.core.sync.PairingResponse
import com.ancata.prima_focus.desktop.db.DesktopDatabaseManager
import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors


class DesktopSyncServer(
    private val dbManager: DesktopDatabaseManager,
    initialPort: Int = 8765,
    private val onSyncCompleted: (Int) -> Unit = {}
) {

    private var server: HttpServer? = null
    private val gson = Gson()
    private val deviceId = UUID.randomUUID().toString()
    private val deviceName = "PrimaFocus Desktop (PC)"

    // Session pairing credentials
    var currentPin: String = generateRandomPin()
        private set
    var sessionKey: String? = null
        private set
    var activePort: Int = initialPort
        private set

    fun generateNewPin(): String {
        currentPin = generateRandomPin()
        return currentPin
    }

    private fun generateRandomPin(): String {
        return (100000..999999).random().toString()
    }

    fun start() {
        var portToTry = activePort
        var bound = false
        var attempts = 0

        while (!bound && attempts < 10) {
            try {
                server = HttpServer.create(InetSocketAddress(portToTry), 0)
                activePort = portToTry
                bound = true
            } catch (e: java.net.BindException) {
                portToTry++
                attempts++
            }
        }

        val s = server ?: throw IllegalStateException("Could not bind HTTP server to any available port in range.")
        s.createContext("/api/pair", PairingHandler())
        s.createContext("/api/sync", SyncDataHandler())
        s.createContext("/api/health", HealthHandler())
        s.executor = Executors.newCachedThreadPool()
        s.start()
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    // Fallback: Export JSON backup for offline/Client Isolation environments
    fun exportBackupJson(): String {
        val tasks = dbManager.getAllTasks()
        val sessions = dbManager.getAllSessions()
        val packet = LANSyncPacket(
            deviceId = deviceId,
            deviceName = deviceName,
            tasks = tasks,
            sessions = sessions
        )
        return gson.toJson(packet)
    }

    // Fallback: Import JSON backup file
    fun importBackupJson(jsonString: String): Int {
        val packet = gson.fromJson(jsonString, LANSyncPacket::class.java)
        return dbManager.mergeSyncPayload(packet.tasks, packet.sessions)
    }

    private inner class HealthHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = """{"status":"ok","deviceId":"$deviceId","deviceName":"$deviceName"}"""
            sendResponse(exchange, 200, response)
        }
    }

    private inner class PairingHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod != "POST") {
                sendResponse(exchange, 405, "Method Not Allowed")
                return
            }

            val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()
            val expectedHash = LANAuthSecurity.hashPin(currentPin)

            try {
                val req = gson.fromJson(body, com.ancata.prima_focus.core.sync.PairingRequest::class.java)
                if (req.pinHash.equals(expectedHash, ignoreCase = true)) {
                    val token = UUID.randomUUID().toString().replace("-", "")
                    sessionKey = token
                    val res = com.ancata.prima_focus.core.sync.PairingResponse(
                        success = true,
                        sessionToken = token,
                        message = "Emparejamiento exitoso con $deviceName"
                    )
                    sendResponse(exchange, 200, gson.toJson(res))
                } else {
                    val res = com.ancata.prima_focus.core.sync.PairingResponse(
                        success = false,
                        message = "PIN inválido. Verifique el código mostrado en la pantalla de su PC."
                    )
                    sendResponse(exchange, 401, gson.toJson(res))
                }
            } catch (e: Exception) {
                sendResponse(exchange, 400, "Bad Request")
            }
        }
    }

    private inner class SyncDataHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod != "POST") {
                sendResponse(exchange, 405, "Method Not Allowed")
                return
            }

            val signatureHeader = exchange.requestHeaders.getFirst("X-Auth-Signature")
            val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()

            // Verify Security / Authentication Gate
            val currentKey = sessionKey
            if (currentKey == null || signatureHeader == null || !LANAuthSecurity.verifyPayloadSignature(body, currentKey, signatureHeader)) {
                sendResponse(exchange, 403, """{"error":"Forbidden: Invalid or missing cryptographic signature"}""")
                return
            }

            try {
                val incomingPacket = gson.fromJson(body, LANSyncPacket::class.java)
                val changes = dbManager.mergeSyncPayload(incomingPacket.tasks, incomingPacket.sessions)
                onSyncCompleted(changes)

                // Return local data back to mobile (Bi-directional sync burst)
                val localTasks = dbManager.getAllTasks()
                val localSessions = dbManager.getAllSessions()
                val responsePacket = LANSyncPacket(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    tasks = localTasks,
                    sessions = localSessions
                )
                val responseJson = gson.toJson(responsePacket)
                val responseSignature = LANAuthSecurity.signPayload(responseJson, currentKey)

                exchange.responseHeaders.set("X-Auth-Signature", responseSignature)
                sendResponse(exchange, 200, responseJson)
            } catch (e: Exception) {
                sendResponse(exchange, 500, "Internal Server Error")
            }
        }
    }

    private fun sendResponse(exchange: HttpExchange, statusCode: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
