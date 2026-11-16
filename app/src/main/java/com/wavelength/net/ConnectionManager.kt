package com.wavelength.net

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the networking layer so the game can run against a real
 * Nearby Connections peer or a local bot without changing any game logic.
 */
interface ConnectionManager {
    val connectionState: StateFlow<ConnectionState>
    val incomingMessages: SharedFlow<GameMessage>

    suspend fun startAdvertising(playerName: String)
    suspend fun startDiscovery(playerName: String)
    suspend fun sendMessage(message: GameMessage)
    fun disconnect()
}
