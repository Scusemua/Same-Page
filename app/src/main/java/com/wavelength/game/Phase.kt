package com.wavelength.game

import kotlinx.serialization.Serializable

@Serializable
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
