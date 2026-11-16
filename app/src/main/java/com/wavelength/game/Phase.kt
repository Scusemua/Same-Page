package com.wavelength.game

import kotlinx.serialization.Serializable

@Serializable
/** Game state machine. Each phase determines which screen is shown and which actions the reducer accepts. */
sealed interface Phase {
    @Serializable
    data object Lobby : Phase

    @Serializable
    data class SubmittingClues(
        val hostDone: Boolean = false,
        val guestDone: Boolean = false
    ) : Phase

    @Serializable
    data class Guessing(val currentRoundIndex: Int) : Phase

    @Serializable
    data class Reveal(val currentRoundIndex: Int) : Phase

    @Serializable
    data object Results : Phase
}
