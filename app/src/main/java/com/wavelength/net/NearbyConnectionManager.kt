package com.wavelength.net

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Device-to-device connection using Google Nearby Connections over Wi-Fi and Bluetooth.
 * Uses P2P_POINT_TO_POINT strategy, which supports exactly two devices.
 */
class NearbyConnectionManager(context: Context) : ConnectionManager {

    private val connectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 64)
    override val incomingMessages: SharedFlow<GameMessage> = _incomingMessages.asSharedFlow()

    // Incremented on each resetRadios() call. Callbacks capture the current value when created
    // and bail out if it's changed, preventing stale callbacks from a previous connection attempt
    // from interfering with a new one.
    private var sessionId = 0
    private var connectedEndpointId: String? = null
    // Set when we've requested a connection but haven't heard back yet.
    // Checked in onEndpointFound to avoid connecting to a second host while the first is pending.
    private var pendingEndpointId: String? = null
    // Nearby callbacks identify peers by opaque endpoint IDs; this maps them to the
    // human-readable names we show in the lobby UI.
    private val endpointNames = mutableMapOf<String, String>()

    private fun createPayloadCallback(activeSession: Int) = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (activeSession != sessionId) return
            val bytes = payload.asBytes() ?: return
            try {
                val message = json.decodeFromString<GameMessage>(bytes.toString(Charsets.UTF_8))
                _incomingMessages.tryEmit(message)
            } catch (_: Exception) {
                // Ignore malformed payloads
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Byte payloads transfer immediately
        }
    }

    private fun createLifecycleCallback(activeSession: Int) = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            if (activeSession != sessionId) return
            endpointNames[endpointId] = info.endpointName
            connectionsClient.acceptConnection(endpointId, createPayloadCallback(activeSession))
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (activeSession != sessionId) return
            pendingEndpointId = null
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    _connectionState.value = ConnectionState.Connected(
                        endpointId = endpointId,
                        endpointName = endpointNames[endpointId] ?: "Player"
                    )
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    endpointNames.remove(endpointId)
                    _connectionState.value = ConnectionState.Error("Connection was rejected.")
                }
                else -> {
                    endpointNames.remove(endpointId)
                    _connectionState.value = ConnectionState.Error(
                        "Connection failed (code ${result.status.statusCode})"
                    )
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (activeSession != sessionId) return
            endpointNames.remove(endpointId)
            if (endpointId == connectedEndpointId) {
                connectedEndpointId = null
                _connectionState.value = ConnectionState.Disconnected(reason = "Other player disconnected")
            }
        }
    }

    override suspend fun startAdvertising(playerName: String) {
        val currentSession = resetRadios()
        _connectionState.value = ConnectionState.Advertising

        connectionsClient.startAdvertising(
            playerName,
            SERVICE_ID,
            createLifecycleCallback(currentSession),
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        ).addOnFailureListener { e ->
            if (currentSession != sessionId) return@addOnFailureListener
            val statusCode = (e as? ApiException)?.statusCode
            if (statusCode != ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) {
                _connectionState.value = ConnectionState.Error("Advertising failed: ${e.message}")
            }
        }
    }

    override suspend fun startDiscovery(playerName: String) {
        val currentSession = resetRadios()
        _connectionState.value = ConnectionState.Discovering

        connectionsClient.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    if (currentSession != sessionId) return
                    if (connectedEndpointId != null || pendingEndpointId != null) return

                    pendingEndpointId = endpointId
                    endpointNames[endpointId] = info.endpointName

                    connectionsClient.requestConnection(
                        playerName,
                        endpointId,
                        createLifecycleCallback(currentSession)
                    ).addOnFailureListener { e ->
                        if (currentSession != sessionId) return@addOnFailureListener
                        pendingEndpointId = null
                        endpointNames.remove(endpointId)
                        connectionsClient.stopDiscovery()
                        _connectionState.value = ConnectionState.Error(
                            "Could not connect to host: ${e.message}"
                        )
                    }
                }

                override fun onEndpointLost(endpointId: String) {
                    if (currentSession != sessionId) return
                    if (pendingEndpointId == endpointId) {
                        pendingEndpointId = null
                    }
                    endpointNames.remove(endpointId)
                }
            },
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build()
        ).addOnFailureListener { e ->
            if (currentSession != sessionId) return@addOnFailureListener
            val statusCode = (e as? ApiException)?.statusCode
            if (statusCode != ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) {
                _connectionState.value = ConnectionState.Error("Discovery failed: ${e.message}")
            }
        }
    }

    override suspend fun sendMessage(message: GameMessage) {
        val endpointId = connectedEndpointId ?: return
        val currentSession = sessionId
        val bytes = json.encodeToString(GameMessage.serializer(), message).toByteArray(Charsets.UTF_8)

        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { e ->
                if (currentSession == sessionId && connectedEndpointId == endpointId) {
                    connectedEndpointId = null
                    _connectionState.value = ConnectionState.Disconnected(
                        reason = "Lost connection: ${e.message ?: "send failed"}"
                    )
                }
            }
    }

    override fun disconnect() {
        resetRadios()
        _connectionState.value = ConnectionState.Idle
    }

    private fun resetRadios(): Int {
        sessionId++
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        pendingEndpointId = null
        endpointNames.clear()
        return sessionId
    }

    companion object {
        private const val SERVICE_ID = "com.wavelength.nearby"
    }
}
