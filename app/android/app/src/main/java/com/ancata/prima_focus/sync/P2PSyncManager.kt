package com.ancata.prima_focus.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ancata.prima_focus.data.local.entity.TaskEntity
import com.ancata.prima_focus.data.local.entity.SessionEntity
import java.nio.charset.StandardCharsets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SyncDataPayload(
    val version: Int = 1,
    val tasks: List<TaskEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList()
)

class P2PSyncManager(
    private val context: Context,
    private val onDataReceived: (List<TaskEntity>, List<SessionEntity>) -> Unit,
    private val suspendGetLocalData: suspend () -> Pair<List<TaskEntity>, List<SessionEntity>>,
    private val onStatusUpdate: (String) -> Unit
) {
    companion object {
        const val SERVICE_ID = "com.ancata.prima_focus.P2P_SYNC"
        const val TAG = "P2PSyncManager"
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private var connectedEndpointId: String? = null
    private val gson = Gson()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val json = payload.asBytes()?.let { String(it, StandardCharsets.UTF_8) }
                if (json != null) {
                    try {
                        val trimmed = json.trim()
                        val (receivedTasks, receivedSessions) = if (trimmed.startsWith("[")) {
                            // Legacy format: raw List<TaskEntity>
                            val listType = object : TypeToken<List<TaskEntity>>() {}.type
                            val tasks: List<TaskEntity> = gson.fromJson(json, listType)
                            Pair(tasks, emptyList<SessionEntity>())
                        } else {
                            // Modern format: SyncDataPayload
                            val dataPayload: SyncDataPayload = gson.fromJson(json, SyncDataPayload::class.java)
                            Pair(dataPayload.tasks ?: emptyList(), dataPayload.sessions ?: emptyList())
                        }
                        
                        Log.d(TAG, "Received ${receivedTasks.size} tasks and ${receivedSessions.size} sessions from $endpointId")
                        onDataReceived(receivedTasks, receivedSessions)
                        onStatusUpdate("Sincronización exitosa (${receivedTasks.size} tareas, ${receivedSessions.size} sesiones)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing sync JSON payload", e)
                        onStatusUpdate("Error al leer datos recibidos")
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with ${info.endpointName}")
            // Automatically accept connection
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            onStatusUpdate("Conectando con ${info.endpointName}...")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connected to $endpointId")
                    connectedEndpointId = endpointId
                    onStatusUpdate("Conectado. Intercambiando datos...")
                    // Stop discovery/advertising once connected
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
            onStatusUpdate("Dispositivo encontrado. Solicitando conexión...")
            connectionsClient.requestConnection("PrimaFocus", endpointId, connectionLifecycleCallback)
                .addOnFailureListener {
                    Log.e(TAG, "Failed to request connection", it)
                    onStatusUpdate("Fallo al solicitar conexión")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        try {
            connectionsClient.startAdvertising("PrimaFocusHost", SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener {
                    Log.d(TAG, "Advertising started")
                    onStatusUpdate("Anfitrión iniciado. Esperando dispositivo...")
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
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        try {
            connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener {
                    Log.d(TAG, "Discovery started")
                    onStatusUpdate("Buscando anfitrión Prima-Focus...")
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

    fun stopAdvertisingAndDiscovery() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
    }

    fun stopAll() {
        stopAdvertisingAndDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
    }

    private fun sendLocalData() {
        val endpoint = connectedEndpointId ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (localTasks, localSessions) = suspendGetLocalData()
                val payloadData = SyncDataPayload(
                    version = 1,
                    tasks = localTasks,
                    sessions = localSessions
                )
                val json = gson.toJson(payloadData)
                val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
                connectionsClient.sendPayload(endpoint, payload)
                Log.d(TAG, "Sent ${localTasks.size} local tasks and ${localSessions.size} sessions to $endpoint")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send local data", e)
            }
        }
    }
}
