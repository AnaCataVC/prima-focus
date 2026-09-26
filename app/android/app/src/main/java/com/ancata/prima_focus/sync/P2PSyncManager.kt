package com.ancata.prima_focus.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.ancata.prima_focus.core.sync.SyncSettings
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.local.entity.SessionEntity
import java.nio.charset.StandardCharsets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wire format exchanged between devices. Version 2 adds the sender's [deviceId] and the synced
 * [settings]; both are optional so version 1 payloads (and the legacy bare task array) still parse.
 */
data class SyncDataPayload(
    val version: Int = CURRENT_PAYLOAD_VERSION,
    val tasks: List<TaskEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val deviceId: String? = null,
    val settings: SyncSettings? = null
) {
    companion object {
        const val CURRENT_PAYLOAD_VERSION = 2
    }
}

/** A connection waiting for the user to compare the authentication digits and accept or reject it. */
data class PendingConnection(
    val endpointId: String,
    val endpointName: String,
    val authDigits: String
)

class P2PSyncManager(
    private val context: Context,
    private val buildLocalPayload: suspend () -> SyncDataPayload,
    /** Merges a received payload and returns the summary shown to the user. */
    private val onPayloadReceived: suspend (SyncDataPayload) -> String,
    private val onStatusUpdate: (String) -> Unit
) {
    companion object {
        const val SERVICE_ID = "com.ancata.prima_focus.P2P_SYNC"
        const val TAG = "P2PSyncManager"
        const val AUTO_TIMEOUT_MS = 45000L // 45 seconds to prevent battery drain
        const val MAX_PAYLOAD_BYTES = 5 * 1024 * 1024L // 5 MB max payload safety limit
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private var connectedEndpointId: String? = null
    private val gson = Gson()
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var timeoutJob: Job? = null

    private val _pendingConnection = MutableStateFlow<PendingConnection?>(null)
    val pendingConnection: StateFlow<PendingConnection?> = _pendingConnection.asStateFlow()

    // Outgoing payload bookkeeping for the single automatic retry.
    private var outgoingPayloadId: Long? = null
    private var outgoingBytes: ByteArray? = null
    private var outgoingRetried = false

    val deviceDisplayName: String = run {
        val model = Build.MODEL ?: "Android Device"
        if (model.length > 25) model.substring(0, 25) else model
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            if (bytes.size > MAX_PAYLOAD_BYTES) {
                Log.e(TAG, "Payload rejected: size (${bytes.size} bytes) exceeds limit ($MAX_PAYLOAD_BYTES bytes)")
                onStatusUpdate("Error: Datos recibidos exceden el límite de seguridad (5MB)")
                return
            }

            val dataPayload = try {
                parsePayload(String(bytes, StandardCharsets.UTF_8))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing sync JSON payload", e)
                onStatusUpdate("Error al leer datos recibidos")
                return
            }

            Log.d(TAG, "Received ${dataPayload.tasks.size} tasks and ${dataPayload.sessions.size} sessions from $endpointId")
            syncScope.launch {
                try {
                    onStatusUpdate(onPayloadReceived(dataPayload))
                } catch (e: Exception) {
                    Log.e(TAG, "Error merging received data", e)
                    onStatusUpdate("Error al combinar los datos recibidos")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val failed = update.status == PayloadTransferUpdate.Status.FAILURE ||
                update.status == PayloadTransferUpdate.Status.CANCELED
            if (!failed) return

            if (update.payloadId != outgoingPayloadId) {
                Log.e(TAG, "Incoming payload ${update.payloadId} failed from $endpointId")
                onStatusUpdate("Error al recibir datos del otro dispositivo")
                return
            }
            val bytes = outgoingBytes
            if (!outgoingRetried && bytes != null) {
                Log.w(TAG, "Outgoing payload failed, retrying once")
                outgoingRetried = true
                onStatusUpdate("Reintentando envío...")
                sendBytes(endpointId, bytes)
            } else {
                Log.e(TAG, "Outgoing payload failed after retry")
                onStatusUpdate("Error al enviar datos. Intenta sincronizar de nuevo")
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with ${info.endpointName}")
            cancelTimeout()
            // Both devices must confirm the same digits before any data is exchanged.
            _pendingConnection.value = PendingConnection(endpointId, info.endpointName, info.authenticationDigits)
            onStatusUpdate("Confirma el código ${info.authenticationDigits} con ${info.endpointName}")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            _pendingConnection.value = null
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connected to $endpointId")
                    connectedEndpointId = endpointId
                    cancelTimeout()
                    onStatusUpdate("Conectado. Intercambiando datos...")
                    // Stop discovery/advertising once connected to conserve radio
                    stopAdvertisingAndDiscovery()

                    // Send local data to the other device
                    sendLocalData()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                    onStatusUpdate("Conexión rechazada")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "Connection error with $endpointId")
                    onStatusUpdate("Error de conexión")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            connectedEndpointId = null
            onStatusUpdate("Desconectado")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: ${info.endpointName}, connecting...")
            onStatusUpdate("Dispositivo encontrado (${info.endpointName}). Conectando...")
            connectionsClient.requestConnection(deviceDisplayName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener {
                    Log.e(TAG, "Failed to request connection", it)
                    onStatusUpdate("Fallo al solicitar conexión")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    fun acceptPendingConnection() {
        val pending = _pendingConnection.value ?: return
        _pendingConnection.value = null
        connectionsClient.acceptConnection(pending.endpointId, payloadCallback)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to accept connection", e)
                onStatusUpdate("Fallo al aceptar la conexión")
            }
        onStatusUpdate("Conectando con ${pending.endpointName}...")
    }

    fun rejectPendingConnection() {
        val pending = _pendingConnection.value ?: return
        _pendingConnection.value = null
        connectionsClient.rejectConnection(pending.endpointId)
        onStatusUpdate("Conexión rechazada")
    }

    fun startAdvertising() {
        stopAll()
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        try {
            connectionsClient.startAdvertising(deviceDisplayName, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener {
                    Log.d(TAG, "Advertising started as $deviceDisplayName")
                    onStatusUpdate("Anfitrión iniciado ($deviceDisplayName). Esperando...")
                    scheduleAutoTimeout("Anfitrión cancelado por inactividad (45s)")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to start advertising", e)
                    onStatusUpdate("Fallo al iniciar anfitrión")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing permissions", e)
            onStatusUpdate("Faltan permisos para sincronizar")
        }
    }

    fun startDiscovery() {
        stopAll()
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        try {
            connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener {
                    Log.d(TAG, "Discovery started as $deviceDisplayName")
                    onStatusUpdate("Buscando dispositivos cercanos (45s)...")
                    scheduleAutoTimeout("Búsqueda finalizada por inactividad (45s)")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to start discovery", e)
                    onStatusUpdate("Fallo al iniciar búsqueda")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing permissions", e)
            onStatusUpdate("Faltan permisos para buscar")
        }
    }

    private fun scheduleAutoTimeout(timeoutMessage: String) {
        cancelTimeout()
        timeoutJob = syncScope.launch {
            delay(AUTO_TIMEOUT_MS)
            Log.d(TAG, "Auto-timeout triggered after ${AUTO_TIMEOUT_MS}ms")
            stopAdvertisingAndDiscovery()
            onStatusUpdate(timeoutMessage)
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    fun stopAdvertisingAndDiscovery() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
    }

    fun stopAll() {
        cancelTimeout()
        stopAdvertisingAndDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        _pendingConnection.value = null
        outgoingPayloadId = null
        outgoingBytes = null
        syncScope.coroutineContext.cancelChildren()
    }

    /** Parses the current payload, a version 1 payload, or the legacy bare task array. */
    private fun parsePayload(json: String): SyncDataPayload {
        if (json.trim().startsWith("[")) {
            val listType = object : TypeToken<List<TaskEntity>>() {}.type
            val tasks: List<TaskEntity> = gson.fromJson(json, listType)
            return SyncDataPayload(version = 0, tasks = tasks)
        }
        val parsed: SyncDataPayload = gson.fromJson(json, SyncDataPayload::class.java)
        // Gson bypasses Kotlin defaults, so absent lists arrive as null.
        @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
        return parsed.copy(tasks = parsed.tasks ?: emptyList(), sessions = parsed.sessions ?: emptyList())
    }

    private fun sendLocalData() {
        val endpoint = connectedEndpointId ?: return
        syncScope.launch {
            try {
                val payloadData = buildLocalPayload()
                val payloadBytes = gson.toJson(payloadData).toByteArray(StandardCharsets.UTF_8)
                if (payloadBytes.size > MAX_PAYLOAD_BYTES) {
                    Log.e(TAG, "Cannot send: payload exceeds limit (${payloadBytes.size} bytes)")
                    onStatusUpdate("Error: Payload local excede el límite (5MB)")
                    return@launch
                }
                outgoingRetried = false
                sendBytes(endpoint, payloadBytes)
                Log.d(TAG, "Sent ${payloadData.tasks.size} local tasks and ${payloadData.sessions.size} sessions to $endpoint")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send local data", e)
                onStatusUpdate("Error al preparar los datos para enviar")
            }
        }
    }

    private fun sendBytes(endpointId: String, bytes: ByteArray) {
        val payload = Payload.fromBytes(bytes)
        outgoingPayloadId = payload.id
        outgoingBytes = bytes
        connectionsClient.sendPayload(endpointId, payload)
            .addOnFailureListener { e ->
                Log.e(TAG, "sendPayload failed", e)
                onStatusUpdate("Error al enviar datos")
            }
    }
}
