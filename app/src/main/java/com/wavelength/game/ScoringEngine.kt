package com.wavelength.game

import kotlin.math.abs

/** Points based on guess-to-target distance. Three concentric zones (bullseye, inner, outer) plus a miss. */
object ScoringEngine {

    fun calculate(target: Float, guess: Float): Int {
        val distance = abs(target - guess)
        return when {
            distance <= GameConfig.BULLSEYE_RADIUS -> 4
            distance <= GameConfig.INNER_RADIUS -> 3
            distance <= GameConfig.OUTER_RADIUS -> 2
            else -> 0
        }
    }

    fun rating(score: Int): String =
        GameConfig.RATING_TABLE.lastOrNull { score >= it.first }?.second
            ?: GameConfig.RATING_TABLE.first().second
}
