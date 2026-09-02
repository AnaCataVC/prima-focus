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
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class DesktopSyncServer(
    private val dbManager: DesktopDatabaseManager,
    initialPort: Int = 8765,
    private val onSyncCompleted: (Int) -> Unit = {}
) {
    constructor(dbManager: DesktopDatabaseManager, port: Int) : this(dbManager, initialPort = port)
    companion object {
        const val MAX_PAYLOAD_BYTES = 5 * 1024 * 1024 // 5 MB safety limit
        const val MAX_FAILED_PAIR_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds lockout after brute-force detection
    }

    private var server: HttpServer? = null
    private var threadPool: ExecutorService? = null
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

    val isRunning: Boolean
        get() = server != null

    // Red-Team security: Rate limiting against PIN brute-forcing
    private val failedPairAttempts = AtomicInteger(0)
    private var lockoutUntilMs = 0L

    fun generateNewPin(resetLockout: Boolean = true): String {
        currentPin = generateRandomPin()
        if (resetLockout) {
            failedPairAttempts.set(0)
            lockoutUntilMs = 0L
        }
        return currentPin
    }

    private fun generateRandomPin(): String {
        return (100000..999999).random().toString()
    }

    @Synchronized
    fun start(): Boolean {
        if (server != null) return true

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

        val s = server ?: return false
        s.createContext("/api/pair", PairingHandler())
        s.createContext("/api/sync", SyncDataHandler())
        s.createContext("/api/health", HealthHandler())

        // Bound pool of 4 workers to prevent JVM thread exhaustion under heavy network activity
        val pool = Executors.newFixedThreadPool(4)
        threadPool = pool
        s.executor = pool
        s.start()
        return true
    }

    @Synchronized
    fun stop() {
        try {
            server?.stop(0)
        } catch (_: Exception) {}
        server = null

        try {
            threadPool?.shutdownNow()
        } catch (_: Exception) {}
        threadPool = null
    }

    /**
     * Resolves the primary local IPv4 address of this machine.
     * Filters out loopback and virtual adapters (WSL, Docker, Hyper-V, VirtualBox, Tailscale).
     */
    fun getLocalIpAddress(): String {
        val candidates = getAllLocalIpAddresses()
        // Prioritize standard 192.168.x.x home/office Wi-Fi, then 10.x.x.x, then 172.x.x.x
        return candidates.firstOrNull { it.startsWith("192.168.") }
            ?: candidates.firstOrNull { it.startsWith("10.") }
            ?: candidates.firstOrNull()
            ?: "127.0.0.1"
    }

    /**
     * Returns all active, non-virtual IPv4 local addresses.
     */
    fun getAllLocalIpAddresses(): List<String> {
        val results = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return results
            for (nif in interfaces) {
                if (nif.isLoopback || !nif.isUp) continue

                val name = nif.name.lowercase()
                val displayName = nif.displayName.lowercase()

                // Ignore virtual switch adapters
                if (name.contains("docker") || displayName.contains("docker") ||
                    name.contains("vethernet") || displayName.contains("vethernet") ||
                    name.contains("wsl") || displayName.contains("wsl") ||
                    name.contains("vbox") || displayName.contains("virtualbox") ||
                    name.contains("tailscale") || displayName.contains("tailscale") ||
                    name.contains("vmware") || displayName.contains("vmware")
                ) {
                    continue
                }

                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val ip = addr.hostAddress
                        if (!results.contains(ip)) {
                            results.add(ip)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return results
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
            val response = """{"status":"ok","deviceId":"$deviceId","deviceName":"$deviceName","port":$activePort}"""
            sendResponse(exchange, 200, response)
        }
    }

    private inner class PairingHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod != "POST") {
                sendResponse(exchange, 405, "Method Not Allowed")
                return
            }

            val now = System.currentTimeMillis()
            if (now < lockoutUntilMs) {
                val remainingSec = ((lockoutUntilMs - now) / 1000).coerceAtLeast(1)
                val res = PairingResponse(
                    success = false,
                    message = "Demasiados intentos fallidos. Bloqueado temporalmente ($remainingSec s)."
                )
                sendResponse(exchange, 429, gson.toJson(res))
                return
            }

            val body = readRequestBody(exchange)
            val expectedHash = LANAuthSecurity.hashPin(currentPin)

            try {
                val req = gson.fromJson(body, PairingRequest::class.java)
                if (req.pinHash.equals(expectedHash, ignoreCase = true)) {
                    // Reset brute force counter
                    failedPairAttempts.set(0)
                    lockoutUntilMs = 0L

                    val token = UUID.randomUUID().toString().replace("-", "")
                    sessionKey = token
                    val res = PairingResponse(
                        success = true,
                        sessionToken = token,
                        message = "Emparejamiento exitoso con $deviceName"
                    )
                    sendResponse(exchange, 200, gson.toJson(res))
                } else {
                    val failures = failedPairAttempts.incrementAndGet()
                    if (failures >= MAX_FAILED_PAIR_ATTEMPTS) {
                        lockoutUntilMs = now + LOCKOUT_DURATION_MS
                        generateNewPin(resetLockout = false) // Invalidate PIN on brute force detection without resetting lockout
                        val res = PairingResponse(
                            success = false,
                            message = "Demasiados intentos erróneos. Servidor bloqueado por 30 segundos y nuevo PIN generado."
                        )
                        sendResponse(exchange, 429, gson.toJson(res))
                    } else {
                        val remaining = MAX_FAILED_PAIR_ATTEMPTS - failures
                        val res = PairingResponse(
                            success = false,
                            message = "PIN inválido. Le quedan $remaining intentos."
                        )
                        sendResponse(exchange, 401, gson.toJson(res))
                    }
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
            val body = readRequestBody(exchange)

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

    private fun readRequestBody(exchange: HttpExchange): String {
        val stream = exchange.requestBody
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var total = 0
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            total += read
            if (total > MAX_PAYLOAD_BYTES) {
                throw IllegalStateException("Payload size exceeded limit ($MAX_PAYLOAD_BYTES bytes)")
            }
            output.write(buffer, 0, read)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    private fun sendResponse(exchange: HttpExchange, statusCode: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
