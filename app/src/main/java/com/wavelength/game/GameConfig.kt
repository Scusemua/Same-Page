package com.wavelength.game

object GameConfig {
    const val CLUES_PER_PLAYER = 3
    const val BULLSEYE_RADIUS = 0.04f
    const val INNER_RADIUS = 0.10f
    const val OUTER_RADIUS = 0.18f

    val RATING_TABLE: List<Pair<Int, String>> = listOf(
        0 to "Are you sure it's plugged in?",
        4 to "Try turning it off and back on",
        8 to "Blow into the bottom of the device",
        12 to "Not bad! Not great, but not bad",
        15 to "So close",
        19 to "You won!",
        21 to "You're on the same page",
        24 to "Galaxy brain"
    )
}
