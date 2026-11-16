package com.wavelength.net

import android.content.Context
import com.wavelength.data.SpectrumRepository
import com.wavelength.game.GameAction
import com.wavelength.game.GameConfig
import com.wavelength.game.GameEngine
import com.wavelength.game.GameState
import com.wavelength.game.Phase
import com.wavelength.game.RoundEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Simulates a second player locally. Operates in two modes depending on who connects:
 * - Local player hosts (startAdvertising): bot acts as guest, submitting random clues and guesses.
 * - Local player joins (startDiscovery): bot acts as host, running the full game loop with
 *   its own GameEngine, creating rounds, writing clues, guessing, and advancing phases.
 *
 * The scope is cancelled and recreated on disconnect to prevent orphaned coroutines from
 * emitting state changes after the player leaves.
 */
class FakeConnectionManager(context: Context) : ConnectionManager {

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val spectrumRepository = SpectrumRepository(context)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<GameMessage>(extraBufferCapacity = 16)
    override val incomingMessages: SharedFlow<GameMessage> = _incomingMessages.asSharedFlow()

    // Guards against re-submitting clues when multiple StateSyncs arrive during the clue phase.
    private var botCluesSubmitted = false
    // Guards against double-guessing the same round from duplicate StateSyncs.
    private var lastGuessedRound = -1

    // The bot-as-host's own copy of game state, independent of the ViewModel's state.
    // Only used in the startDiscovery (bot-as-host) path.
    private var simulatedHostState = GameState.initial()

    override suspend fun startAdvertising(playerName: String) {
        _connectionState.value = ConnectionState.Advertising
        delay(800)
        _connectionState.value = ConnectionState.Connected(
            endpointId = "fake-guest-id",
            endpointName = "Test Bot (Guest)"
        )
    }

    override suspend fun startDiscovery(playerName: String) {
        _connectionState.value = ConnectionState.Discovering
        delay(800)
        _connectionState.value = ConnectionState.Connected(
            endpointId = "fake-host-id",
            endpointName = "Test Bot (Host)"
        )
        startBotHostGame()
    }

    private fun startBotHostGame() {
        scope.launch {
            delay(1000)
            val totalCards = GameConfig.CLUES_PER_PLAYER * 2
            val cards = spectrumRepository.drawShuffled(totalCards)
            val rounds = (0 until GameConfig.CLUES_PER_PLAYER).flatMap { i ->
                listOf(
                    RoundEntry(card = cards[i * 2], targetPosition = Random.nextFloat(), psychicIsHost = true),
                    RoundEntry(card = cards[i * 2 + 1], targetPosition = Random.nextFloat(), psychicIsHost = false)
                )
            }

            // Apply both actions before syncing so the guest never sees empty rounds
            var state = GameState.initial()
            state = GameEngine.reduce(state, GameAction.StartGame)
            state = GameEngine.reduce(state, GameAction.InitializeRounds(rounds))
            simulatedHostState = state
            syncToLocalGuest()

            // Bot Host submits its clues
            delay(1000)
            val availableClues = SAMPLE_CLUES.shuffled().toMutableList()
            simulatedHostState.rounds.forEachIndexed { index, round ->
                if (round.psychicIsHost) {
                    val word = availableClues.removeFirstOrNull() ?: SAMPLE_CLUES.random()
                    simulatedHostState = GameEngine.reduce(
                        simulatedHostState,
                        GameAction.SubmitClue(roundIndex = index, clue = word, isHost = true)
                    )
                }
            }
            simulatedHostState = GameEngine.reduce(
                simulatedHostState,
                GameAction.PlayerFinishedClues(isHost = true)
            )
            syncToLocalGuest()
        }
    }

    // Routing depends on which mode we're in. StateSync means the local player is host
    // and is pushing state to us (bot-as-guest). Action means the local player is guest
    // and is sending their move to us (bot-as-host).
    override suspend fun sendMessage(message: GameMessage) {
        when (message) {
            is GameMessage.StateSync -> handleHostStateSync(message)
            is GameMessage.Action -> handleGuestActionAsHostBot(message.action)
            is GameMessage.GuessPreview -> { /* Bot has no UI to display previews */ }
        }
    }

    private fun handleGuestActionAsHostBot(action: GameAction) {
        scope.launch {
            simulatedHostState = GameEngine.reduce(simulatedHostState, action)
            syncToLocalGuest()
            processBotHostTurns()
        }
    }

    private suspend fun processBotHostTurns() {
        while (true) {
            when (val phase = simulatedHostState.phase) {
                is Phase.Guessing -> {
                    val round = simulatedHostState.rounds.getOrNull(phase.currentRoundIndex)
                    if (round != null && !round.psychicIsHost) {
                        val finalPos = Random.nextFloat()
                        emitBotGuessPreview(phase.currentRoundIndex, finalPos)
                        simulatedHostState = GameEngine.reduce(
                            simulatedHostState,
                            GameAction.SubmitGuess(phase.currentRoundIndex, finalPos)
                        )
                        syncToLocalGuest()
                    } else {
                        break
                    }
                }

                is Phase.Reveal -> {
                    // Pause so you can inspect the Reveal screen, then advance as Host Bot
                    delay(3000)
                    simulatedHostState = GameEngine.reduce(
                        simulatedHostState,
                        GameAction.AdvanceToNextRound
                    )
                    syncToLocalGuest()
                    // Loop continues in case the next round is another Guest-psychic round
                }

                is Phase.Lobby -> {
                    startBotHostGame()
                    break
                }

                else -> break
            }
        }
    }

    private suspend fun syncToLocalGuest() {
        // Redact Host target positions just like GameViewModel.redactForGuest() does
        val redacted = simulatedHostState.copy(
            rounds = simulatedHostState.rounds.map { entry ->
                if (entry.psychicIsHost) {
                    when {
                        entry.pointsAwarded != null -> entry
                        simulatedHostState.phase is Phase.SubmittingClues -> entry.copy(card = null, targetPosition = null)
                        else -> entry.copy(targetPosition = null)
                    }
                } else {
                    entry
                }
            }
        )
        _incomingMessages.emit(GameMessage.StateSync(redacted))
    }

    private fun handleHostStateSync(sync: GameMessage.StateSync) {
        val state = sync.state
        when (val phase = state.phase) {
            is Phase.SubmittingClues -> {
                if (!botCluesSubmitted && state.rounds.isNotEmpty()) {
                    botCluesSubmitted = true
                    scope.launch {
                        delay(1000)
                        val availableClues = SAMPLE_CLUES.shuffled().toMutableList()
                        state.rounds.forEachIndexed { index, round ->
                            if (!round.psychicIsHost) {
                                val randomWord = availableClues.removeFirstOrNull() ?: SAMPLE_CLUES.random()
                                _incomingMessages.emit(
                                    GameMessage.Action(
                                        GameAction.SubmitClue(
                                            roundIndex = index,
                                            clue = randomWord,
                                            isHost = false
                                        )
                                    )
                                )
                            }
                        }
                        delay(400)
                        _incomingMessages.emit(
                            GameMessage.Action(
                                GameAction.PlayerFinishedClues(isHost = false)
                            )
                        )
                    }
                }
            }

            is Phase.Guessing -> {
                val roundIndex = state.rounds.indexOfFirst { it.guessPosition == null }
                val currentRound = state.rounds.getOrNull(roundIndex)

                if (currentRound != null && currentRound.psychicIsHost && lastGuessedRound != roundIndex) {
                    lastGuessedRound = roundIndex
                    scope.launch {
                        val finalPos = Random.nextFloat()
                        emitBotGuessPreview(roundIndex, finalPos)
                        _incomingMessages.emit(
                            GameMessage.Action(
                                GameAction.SubmitGuess(
                                    roundIndex = roundIndex,
                                    position = finalPos
                                )
                            )
                        )
                    }
                }
            }

            else -> {
                if (phase is Phase.Lobby) {
                    botCluesSubmitted = false
                    lastGuessedRound = -1
                }
            }
        }
    }

    override fun disconnect() {
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        botCluesSubmitted = false
        lastGuessedRound = -1
        simulatedHostState = GameState.initial()
        _connectionState.value = ConnectionState.Disconnected("Local test ended")
    }

    private suspend fun emitBotGuessPreview(roundIndex: Int, finalPosition: Float) {
        val start = 0.5f
        val steps = 15
        val stepDelay = 100L
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val smoothT = t * t * (3f - 2f * t)
            val pos = start + (finalPosition - start) * smoothT
            _incomingMessages.emit(GameMessage.GuessPreview(roundIndex, pos))
            delay(stepDelay)
        }
    }

    companion object {
        private val SAMPLE_CLUES = listOf(
            "Pineapple", "Volcano", "Submarine", "Cactus", "Telescope",
            "Pancake", "Avalanche", "Origami", "Thunderstorm", "Espresso",
            "Chameleon", "Trampoline", "Lighthouse", "Marshmallow", "Glacier",
            "Porcupine", "Saxophone", "Quicksand", "Hammock", "Meteor",
            "Cinnamon", "Penguin", "Labyrinth", "Popcorn", "Flamingo",
            "Tornado", "Croissant", "Pendulum", "Jellyfish", "Campfire"
        )
    }
}
