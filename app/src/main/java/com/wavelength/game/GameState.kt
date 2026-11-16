package com.wavelength.game

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val phase: Phase,
    val rounds: List<RoundEntry>,
    val totalScore: Int
) {
    companion object {
        fun initial() = GameState(Phase.Lobby, emptyList(), 0)
    }
}

@Serializable
data class RoundEntry(
    val card: SpectrumCard?,
    val targetPosition: Float?,
    val psychicIsHost: Boolean,
    val clue: String? = null,
    val guessPosition: Float? = null,
    val pointsAwarded: Int? = null
)
