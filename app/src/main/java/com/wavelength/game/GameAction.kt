package com.wavelength.game

import kotlinx.serialization.Serializable

@Serializable
sealed interface GameAction {
    @Serializable
    data object StartGame : GameAction

    @Serializable
    data class InitializeRounds(val rounds: List<RoundEntry>) : GameAction

    @Serializable
    data class SubmitClue(val roundIndex: Int, val clue: String, val isHost: Boolean) : GameAction

    @Serializable
    data class PlayerFinishedClues(val isHost: Boolean) : GameAction

    @Serializable
    data class SubmitGuess(val roundIndex: Int, val position: Float) : GameAction

    @Serializable
    data object AdvanceToNextRound : GameAction

    @Serializable
    data object PlayAgain : GameAction
}
