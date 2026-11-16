package com.wavelength.net

import com.wavelength.game.GameAction
import com.wavelength.game.GameState
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameMessage {
    @Serializable
    data class StateSync(val state: GameState) : GameMessage

    @Serializable
    data class Action(val action: GameAction) : GameMessage
}

enum class ConnectionRole { HOST, GUEST }

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Advertising : ConnectionState
    data object Discovering : ConnectionState
    data class Connected(val endpointId: String, val endpointName: String) : ConnectionState
    data class Disconnected(val reason: String?) : ConnectionState
    data class Error(val message: String) : ConnectionState
}
