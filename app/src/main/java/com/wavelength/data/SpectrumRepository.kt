package com.wavelength.data

import android.content.Context
import com.wavelength.game.SpectrumCard
import kotlinx.serialization.json.Json

class SpectrumRepository(private val context: Context) {

    private var cards: List<SpectrumCard> = emptyList()

    fun load(): List<SpectrumCard> {
        if (cards.isEmpty()) {
            val json = context.assets.open("spectrums.json").bufferedReader().use { it.readText() }
            cards = Json.decodeFromString<List<SpectrumCard>>(json)
        }
        return cards
    }

    fun drawShuffled(count: Int): List<SpectrumCard> =
        load().shuffled().take(count)
}
