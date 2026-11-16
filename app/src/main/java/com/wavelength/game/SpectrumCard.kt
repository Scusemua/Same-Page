package com.wavelength.game

import kotlinx.serialization.Serializable

@Serializable
data class SpectrumCard(
    val id: Int,
    val leftLabel: String,
    val rightLabel: String
)
