package com.wavelength.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.data.SpectrumRepository
import com.wavelength.game.GameAction
import com.wavelength.game.GameConfig
import com.wavelength.game.GameEngine
import com.wavelength.game.GameState
import com.wavelength.game.Phase
import com.wavelength.game.RoundEntry
import com.wavelength.net.ConnectionManager
import com.wavelength.net.ConnectionRole
import com.wavelength.net.ConnectionState
import com.wavelength.net.FakeConnectionManager
import com.wavelength.net.GameMessage
import com.wavelength.net.NearbyConnectionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _useFakeConnection = MutableStateFlow(true)
    val useFakeConnection: StateFlow<Boolean> = _useFakeConnection.asStateFlow()

    private var connectionManager: ConnectionManager = FakeConnectionManager(application)

    private val spectrumRepository = SpectrumRepository(application)

    private val _gameState = MutableStateFlow(GameState.initial())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var messageJob: Job? = null
    private var stateJob: Job? = null

    var localRole: ConnectionRole = ConnectionRole.HOST
        private set

    init {
        startCollecting()
    }

    fun setUseFakeConnection(useFake: Boolean) {
        if (_useFakeConnection.value == useFake) return
        connectionManager.disconnect()
        _useFakeConnection.value = useFake
        connectionManager = if (useFake) {
            FakeConnectionManager(getApplication())
        } else {
            NearbyConnectionManager(getApplication())
        }
        startCollecting()
    }

    private fun startCollecting() {
        messageJob?.cancel()
        stateJob?.cancel()
        messageJob = viewModelScope.launch {
            connectionManager.incomingMessages.collect(::handleMessage)
        }
        stateJob = viewModelScope.launch {
            connectionManager.connectionState.collect { _connectionState.value = it }
        }
    }

    fun hostGame() {
        localRole = ConnectionRole.HOST
        viewModelScope.launch { connectionManager.startAdvertising("Host") }
    }

    fun joinGame() {
        localRole = ConnectionRole.GUEST
        viewModelScope.launch { connectionManager.startDiscovery("Guest") }
    }

    fun startGame() {
        dispatch(GameAction.StartGame)
        initializeRounds()
    }

    fun submitClue(roundIndex: Int, clue: String) {
        dispatch(GameAction.SubmitClue(roundIndex, clue, isHost = localRole == ConnectionRole.HOST))
    }

    fun finishClues() {
        dispatch(GameAction.PlayerFinishedClues(isHost = localRole == ConnectionRole.HOST))
    }

    fun submitGuess(roundIndex: Int, position: Float) {
        dispatch(GameAction.SubmitGuess(roundIndex, position))
    }

    fun advanceToNextRound() {
        dispatch(GameAction.AdvanceToNextRound)
    }

    fun playAgain() {
        dispatch(GameAction.PlayAgain)
    }

    fun disconnect() {
        connectionManager.disconnect()
        _gameState.value = GameState.initial()
    }

    private fun dispatch(action: GameAction) {
        when (localRole) {
            ConnectionRole.HOST -> applyAndSync(action)
            ConnectionRole.GUEST -> viewModelScope.launch {
                connectionManager.sendMessage(GameMessage.Action(action))
            }
        }
    }

    private fun handleMessage(msg: GameMessage) {
        when (msg) {
            is GameMessage.StateSync -> _gameState.value = msg.state
            is GameMessage.Action -> applyAndSync(msg.action)
        }
    }

    private fun applyAndSync(action: GameAction) {
        val newState = GameEngine.reduce(_gameState.value, action)
        _gameState.value = newState
        viewModelScope.launch {
            connectionManager.sendMessage(GameMessage.StateSync(newState.redactForGuest()))
        }
    }

    private fun initializeRounds() {
        val cluesPerPlayer = GameConfig.CLUES_PER_PLAYER
        val totalCards = cluesPerPlayer * 2
        val cards = spectrumRepository.drawShuffled(totalCards)

        val rounds = mutableListOf<RoundEntry>()
        for (i in 0 until cluesPerPlayer) {
            rounds.add(
                RoundEntry(
                    card = cards[i * 2],
                    targetPosition = Random.nextFloat(),
                    psychicIsHost = true
                )
            )
            rounds.add(
                RoundEntry(
                    card = cards[i * 2 + 1],
                    targetPosition = Random.nextFloat(),
                    psychicIsHost = false
                )
            )
        }

        dispatch(GameAction.InitializeRounds(rounds))
    }

    private fun GameState.redactForGuest(): GameState {
        val currentPhase = this.phase
        return copy(rounds = rounds.map { entry ->
            if (entry.psychicIsHost) {
                when {
                    entry.pointsAwarded != null -> entry
                    currentPhase is Phase.SubmittingClues -> entry.copy(card = null, targetPosition = null)
                    else -> entry.copy(targetPosition = null)
                }
            } else {
                entry
            }
        })
    }
}
