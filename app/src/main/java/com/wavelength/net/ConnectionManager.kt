package com.wavelength.net

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ConnectionManager {
    val connectionState: StateFlow<ConnectionState>
    val incomingMessages: SharedFlow<GameMessage>

    suspend fun startAdvertising(playerName: String)
    suspend fun startDiscovery(playerName: String)
    suspend fun sendMessage(message: GameMessage)
    fun disconnect()
}
