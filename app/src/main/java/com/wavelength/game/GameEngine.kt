package com.wavelength.game

/**
 * Pure reducer for all game state transitions. Stateless and side-effect-free so the host
 * and guest can both run the same logic deterministically against the same inputs.
 */
object GameEngine {

    fun reduce(state: GameState, action: GameAction): GameState = when (action) {
        is GameAction.StartGame -> handleStartGame(state)
        is GameAction.InitializeRounds -> handleInitializeRounds(state, action)
        is GameAction.SubmitClue -> handleSubmitClue(state, action)
        is GameAction.PlayerFinishedClues -> handlePlayerFinishedClues(state, action)
        is GameAction.SubmitGuess -> handleSubmitGuess(state, action)
        is GameAction.AdvanceToNextRound -> handleAdvanceToNextRound(state)
        is GameAction.PlayAgain -> handlePlayAgain(state)
    }

    private fun handleStartGame(state: GameState): GameState {
        if (state.phase !is Phase.Lobby) return state
        return state.copy(phase = Phase.SubmittingClues())
    }

    private fun handleInitializeRounds(state: GameState, action: GameAction.InitializeRounds): GameState {
        if (state.phase !is Phase.SubmittingClues) return state
        if (state.rounds.isNotEmpty()) return state
        return state.copy(rounds = action.rounds)
    }

    private fun handleSubmitClue(state: GameState, action: GameAction.SubmitClue): GameState {
        val phase = state.phase as? Phase.SubmittingClues ?: return state
        val round = state.rounds.getOrNull(action.roundIndex) ?: return state

        if (round.psychicIsHost != action.isHost) return state
        if (round.clue != null) return state

        val updatedRounds = state.rounds.toMutableList()
        updatedRounds[action.roundIndex] = round.copy(clue = action.clue)

        return state.copy(
            phase = phase,
            rounds = updatedRounds
        )
    }

    private fun handlePlayerFinishedClues(state: GameState, action: GameAction.PlayerFinishedClues): GameState {
        val phase = state.phase as? Phase.SubmittingClues ?: return state

        val updatedPhase = if (action.isHost) {
            phase.copy(hostDone = true)
        } else {
            phase.copy(guestDone = true)
        }

        return if (updatedPhase.hostDone && updatedPhase.guestDone) {
            state.copy(phase = Phase.Guessing(currentRoundIndex = 0))
        } else {
            state.copy(phase = updatedPhase)
        }
    }

    private fun handleSubmitGuess(state: GameState, action: GameAction.SubmitGuess): GameState {
        val phase = state.phase as? Phase.Guessing ?: return state
        if (action.roundIndex != phase.currentRoundIndex) return state

        val round = state.rounds.getOrNull(action.roundIndex) ?: return state
        if (round.guessPosition != null) return state

        val target = round.targetPosition ?: return state
        val points = ScoringEngine.calculate(target, action.position)

        val updatedRounds = state.rounds.toMutableList()
        updatedRounds[action.roundIndex] = round.copy(
            guessPosition = action.position,
            pointsAwarded = points
        )

        return state.copy(
            phase = Phase.Reveal(currentRoundIndex = phase.currentRoundIndex),
            rounds = updatedRounds,
            totalScore = state.totalScore + points
        )
    }

    private fun handleAdvanceToNextRound(state: GameState): GameState {
        val phase = state.phase as? Phase.Reveal ?: return state
        val nextIndex = phase.currentRoundIndex + 1

        return if (nextIndex < state.rounds.size) {
            state.copy(phase = Phase.Guessing(currentRoundIndex = nextIndex))
        } else {
            state.copy(phase = Phase.Results)
        }
    }

    private fun handlePlayAgain(state: GameState): GameState {
        if (state.phase !is Phase.Results) return state
        return GameState.initial()
    }
}
