package com.wavelength.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringEngineTest {

    @Test
    fun `bullseye at exact target returns 3`() {
        assertEquals(3, ScoringEngine.calculate(0.5f, 0.5f))
    }

    @Test
    fun `bullseye at edge of radius returns 3`() {
        assertEquals(3, ScoringEngine.calculate(0.5f, 0.5f + GameConfig.BULLSEYE_RADIUS))
    }

    @Test
    fun `inner zone just outside bullseye returns 3`() {
        assertEquals(3, ScoringEngine.calculate(0.5f, 0.5f + GameConfig.BULLSEYE_RADIUS + 0.001f))
    }

    @Test
    fun `inner zone at edge returns 3`() {
        assertEquals(3, ScoringEngine.calculate(0.5f, 0.5f + GameConfig.INNER_RADIUS))
    }

    @Test
    fun `outer zone just outside inner returns 2`() {
        assertEquals(2, ScoringEngine.calculate(0.5f, 0.5f + GameConfig.INNER_RADIUS + 0.001f))
    }

    @Test
    fun `outer zone at edge returns 2`() {
        assertEquals(2, ScoringEngine.calculate(0.5f, 0.5f + GameConfig.OUTER_RADIUS))
    }

    @Test
    fun `miss just outside outer returns 0`() {
        assertEquals(0, ScoringEngine.calculate(0.5f, 0.5f + GameConfig.OUTER_RADIUS + 0.001f))
    }

    @Test
    fun `complete miss returns 0`() {
        assertEquals(0, ScoringEngine.calculate(0.0f, 1.0f))
    }

    @Test
    fun `scoring works symmetrically for left side`() {
        assertEquals(3, ScoringEngine.calculate(0.5f, 0.5f - GameConfig.BULLSEYE_RADIUS))
        assertEquals(2, ScoringEngine.calculate(0.5f, 0.5f - GameConfig.OUTER_RADIUS))
    }

    @Test
    fun `scoring works at extremes of spectrum`() {
        assertEquals(3, ScoringEngine.calculate(0.0f, 0.0f))
        assertEquals(3, ScoringEngine.calculate(1.0f, 1.0f))
        assertEquals(3, ScoringEngine.calculate(0.0f, GameConfig.BULLSEYE_RADIUS))
    }

    @Test
    fun `rating returns lowest tier for zero`() {
        assertEquals("Are you sure it's plugged in?", ScoringEngine.rating(0))
    }

    @Test
    fun `rating returns correct tier for mid score`() {
        assertEquals("Not bad! Not great, but not bad", ScoringEngine.rating(9))
        assertEquals("Not bad! Not great, but not bad", ScoringEngine.rating(10))
    }

    @Test
    fun `rating returns top tier for perfect score`() {
        assertEquals("Galaxy brain", ScoringEngine.rating(18))
    }

    @Test
    fun `rating returns correct tier at boundaries`() {
        assertEquals("Are you sure it's plugged in?", ScoringEngine.rating(2))
        assertEquals("Try turning it off and back on", ScoringEngine.rating(3))
        assertEquals("You won!", ScoringEngine.rating(14))
        assertEquals("You won!", ScoringEngine.rating(15))
        assertEquals("You're on the same wavelength", ScoringEngine.rating(16))
    }
}
