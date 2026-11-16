package com.wavelength.game

import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {

    private val testCard = SpectrumCard(1, "Hot", "Cold")
    private val testCard2 = SpectrumCard(2, "Loud", "Quiet")

    private fun makeRounds(): List<RoundEntry> = listOf(
        RoundEntry(card = testCard, targetPosition = 0.3f, psychicIsHost = true),
        RoundEntry(card = testCard2, targetPosition = 0.7f, psychicIsHost = false),
        RoundEntry(card = testCard, targetPosition = 0.5f, psychicIsHost = true),
        RoundEntry(card = testCard2, targetPosition = 0.2f, psychicIsHost = false),
        RoundEntry(card = testCard, targetPosition = 0.8f, psychicIsHost = true),
        RoundEntry(card = testCard2, targetPosition = 0.1f, psychicIsHost = false),
    )

    // --- StartGame ---

    @Test
    fun `StartGame from Lobby transitions to SubmittingClues`() {
        val state = GameState.initial()
        val result = GameEngine.reduce(state, GameAction.StartGame)
        assertTrue(result.phase is Phase.SubmittingClues)
    }

    @Test
    fun `StartGame from non-Lobby is ignored`() {
        val state = GameState.initial().copy(phase = Phase.Results)
        val result = GameEngine.reduce(state, GameAction.StartGame)
        assertSame(state, result)
    }

    // --- InitializeRounds ---

    @Test
    fun `InitializeRounds populates rounds list`() {
        val state = GameState.initial().copy(phase = Phase.SubmittingClues())
        val rounds = makeRounds()
        val result = GameEngine.reduce(state, GameAction.InitializeRounds(rounds))
        assertEquals(6, result.rounds.size)
    }

    @Test
    fun `InitializeRounds is ignored if rounds already exist`() {
        val rounds = makeRounds()
        val state = GameState(Phase.SubmittingClues(), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.InitializeRounds(rounds))
        assertSame(state, result)
    }

    @Test
    fun `InitializeRounds is ignored outside SubmittingClues`() {
        val state = GameState.initial()
        val result = GameEngine.reduce(state, GameAction.InitializeRounds(makeRounds()))
        assertSame(state, result)
    }

    // --- SubmitClue ---

    @Test
    fun `SubmitClue sets clue on correct round`() {
        val rounds = makeRounds()
        val state = GameState(Phase.SubmittingClues(), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.SubmitClue(0, "Lava", isHost = true))
        assertEquals("Lava", result.rounds[0].clue)
        assertNull(result.rounds[1].clue)
    }

    @Test
    fun `SubmitClue is rejected if isHost does not match psychicIsHost`() {
        val rounds = makeRounds()
        val state = GameState(Phase.SubmittingClues(), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.SubmitClue(0, "Lava", isHost = false))
        assertSame(state, result)
    }

    @Test
    fun `SubmitClue is rejected if clue already exists`() {
        val rounds = makeRounds().toMutableList()
        rounds[0] = rounds[0].copy(clue = "Existing")
        val state = GameState(Phase.SubmittingClues(), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.SubmitClue(0, "New", isHost = true))
        assertEquals("Existing", result.rounds[0].clue)
    }

    @Test
    fun `SubmitClue is ignored outside SubmittingClues`() {
        val state = GameState(Phase.Guessing(0), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.SubmitClue(0, "Lava", isHost = true))
        assertSame(state, result)
    }

    // --- PlayerFinishedClues ---

    @Test
    fun `PlayerFinishedClues sets host done flag`() {
        val state = GameState(Phase.SubmittingClues(), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.PlayerFinishedClues(isHost = true))
        val phase = result.phase as Phase.SubmittingClues
        assertTrue(phase.hostDone)
        assertFalse(phase.guestDone)
    }

    @Test
    fun `PlayerFinishedClues sets guest done flag`() {
        val state = GameState(Phase.SubmittingClues(), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.PlayerFinishedClues(isHost = false))
        val phase = result.phase as Phase.SubmittingClues
        assertFalse(phase.hostDone)
        assertTrue(phase.guestDone)
    }

    @Test
    fun `PlayerFinishedClues transitions to Guessing when both done`() {
        val state = GameState(Phase.SubmittingClues(hostDone = true), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.PlayerFinishedClues(isHost = false))
        val phase = result.phase as Phase.Guessing
        assertEquals(0, phase.currentRoundIndex)
    }

    @Test
    fun `PlayerFinishedClues is ignored outside SubmittingClues`() {
        val state = GameState(Phase.Guessing(0), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.PlayerFinishedClues(isHost = true))
        assertSame(state, result)
    }

    // --- SubmitGuess ---

    @Test
    fun `SubmitGuess computes score and transitions to Reveal`() {
        val rounds = makeRounds().toMutableList()
        rounds[0] = rounds[0].copy(clue = "Lava")
        val state = GameState(Phase.Guessing(0), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.SubmitGuess(0, 0.3f))

        val phase = result.phase as Phase.Reveal
        assertEquals(0, phase.currentRoundIndex)
        assertEquals(3, result.rounds[0].pointsAwarded)
        assertEquals(0.3f, result.rounds[0].guessPosition)
        assertEquals(3, result.totalScore)
    }

    @Test
    fun `SubmitGuess with miss scores 0`() {
        val rounds = makeRounds().toMutableList()
        rounds[0] = rounds[0].copy(clue = "Lava")
        val state = GameState(Phase.Guessing(0), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.SubmitGuess(0, 0.9f))
        assertEquals(0, result.rounds[0].pointsAwarded)
        assertEquals(0, result.totalScore)
    }

    @Test
    fun `SubmitGuess is rejected for wrong round index`() {
        val rounds = makeRounds().toMutableList()
        rounds[0] = rounds[0].copy(clue = "Lava")
        val state = GameState(Phase.Guessing(0), rounds, 0)
        val result = GameEngine.reduce(state, GameAction.SubmitGuess(1, 0.5f))
        assertSame(state, result)
    }

    @Test
    fun `SubmitGuess is rejected if already guessed`() {
        val rounds = makeRounds().toMutableList()
        rounds[0] = rounds[0].copy(clue = "Lava", guessPosition = 0.5f, pointsAwarded = 2)
        val state = GameState(Phase.Guessing(0), rounds, 2)
        val result = GameEngine.reduce(state, GameAction.SubmitGuess(0, 0.6f))
        assertSame(state, result)
    }

    @Test
    fun `SubmitGuess is ignored outside Guessing`() {
        val state = GameState(Phase.Reveal(0), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.SubmitGuess(0, 0.5f))
        assertSame(state, result)
    }

    // --- AdvanceToNextRound ---

    @Test
    fun `AdvanceToNextRound moves to next Guessing`() {
        val state = GameState(Phase.Reveal(0), makeRounds(), 3)
        val result = GameEngine.reduce(state, GameAction.AdvanceToNextRound)
        val phase = result.phase as Phase.Guessing
        assertEquals(1, phase.currentRoundIndex)
    }

    @Test
    fun `AdvanceToNextRound transitions to Results after last round`() {
        val state = GameState(Phase.Reveal(5), makeRounds(), 10)
        val result = GameEngine.reduce(state, GameAction.AdvanceToNextRound)
        assertTrue(result.phase is Phase.Results)
    }

    @Test
    fun `AdvanceToNextRound is ignored outside Reveal`() {
        val state = GameState(Phase.Guessing(0), makeRounds(), 0)
        val result = GameEngine.reduce(state, GameAction.AdvanceToNextRound)
        assertSame(state, result)
    }

    // --- PlayAgain ---

    @Test
    fun `PlayAgain resets to initial state`() {
        val state = GameState(Phase.Results, makeRounds(), 15)
        val result = GameEngine.reduce(state, GameAction.PlayAgain)
        assertEquals(GameState.initial(), result)
    }

    @Test
    fun `PlayAgain is ignored outside Results`() {
        val state = GameState(Phase.Guessing(2), makeRounds(), 5)
        val result = GameEngine.reduce(state, GameAction.PlayAgain)
        assertSame(state, result)
    }

    // --- Full game flow ---

    @Test
    fun `full game flow produces expected final state`() {
        var state = GameState.initial()

        state = GameEngine.reduce(state, GameAction.StartGame)
        assertTrue(state.phase is Phase.SubmittingClues)

        state = GameEngine.reduce(state, GameAction.InitializeRounds(makeRounds()))
        assertEquals(6, state.rounds.size)

        state = GameEngine.reduce(state, GameAction.SubmitClue(0, "Lava", isHost = true))
        state = GameEngine.reduce(state, GameAction.SubmitClue(2, "Warm", isHost = true))
        state = GameEngine.reduce(state, GameAction.SubmitClue(4, "Fire", isHost = true))
        state = GameEngine.reduce(state, GameAction.PlayerFinishedClues(isHost = true))
        assertTrue(state.phase is Phase.SubmittingClues)
        assertTrue((state.phase as Phase.SubmittingClues).hostDone)

        state = GameEngine.reduce(state, GameAction.SubmitClue(1, "Thunder", isHost = false))
        state = GameEngine.reduce(state, GameAction.SubmitClue(3, "Whisper", isHost = false))
        state = GameEngine.reduce(state, GameAction.SubmitClue(5, "Silence", isHost = false))
        state = GameEngine.reduce(state, GameAction.PlayerFinishedClues(isHost = false))
        assertTrue(state.phase is Phase.Guessing)

        for (i in 0 until 6) {
            val target = state.rounds[i].targetPosition!!
            state = GameEngine.reduce(state, GameAction.SubmitGuess(i, target))
            assertTrue(state.phase is Phase.Reveal)
            assertEquals(3, state.rounds[i].pointsAwarded)

            if (i < 5) {
                state = GameEngine.reduce(state, GameAction.AdvanceToNextRound)
                assertTrue(state.phase is Phase.Guessing)
            }
        }

        state = GameEngine.reduce(state, GameAction.AdvanceToNextRound)
        assertTrue(state.phase is Phase.Results)
        assertEquals(18, state.totalScore)
    }
}
