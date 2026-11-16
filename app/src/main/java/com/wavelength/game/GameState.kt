package com.wavelength.game

import kotlinx.serialization.Serializable

/**
 * Complete snapshot of a game in progress. Serializable so the host can send it
 * over the wire to the guest. The host holds the authoritative copy; the guest
 * receives a redacted version with hidden targets stripped out.
 */
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

/**
 * One round's data. Fields like [targetPosition] and [card] may be null in the
 * guest's copy to prevent the guesser from seeing the answer before the reveal.
 */
@Serializable
data class RoundEntry(
    val card: SpectrumCard?,
    val targetPosition: Float?,
    val psychicIsHost: Boolean,
    val clue: String? = null,
    val guessPosition: Float? = null,
    val pointsAwarded: Int? = null
)
