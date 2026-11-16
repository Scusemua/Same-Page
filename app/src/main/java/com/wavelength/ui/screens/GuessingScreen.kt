package com.wavelength.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wavelength.game.GameState
import com.wavelength.game.Phase
import com.wavelength.ui.components.SpectrumDial
import com.wavelength.ui.components.SpectrumLabel

@Composable
fun GuessingScreen(
    gameState: GameState,
    isHost: Boolean,
    onSubmitGuess: (roundIndex: Int, position: Float) -> Unit,
    onLeaveGame: () -> Unit
) {
    val phase = gameState.phase as? Phase.Guessing ?: return
    val round = gameState.rounds.getOrNull(phase.currentRoundIndex) ?: return
    val card = round.card ?: return

    val isGuesser = round.psychicIsHost != isHost
    val roundNumber = phase.currentRoundIndex + 1
    val totalRounds = gameState.rounds.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Score: ${gameState.totalScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = onLeaveGame) {
                    Text("Leave Game")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Round $roundNumber of $totalRounds",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SpectrumLabel(leftLabel = card.leftLabel, rightLabel = card.rightLabel)

            Spacer(modifier = Modifier.height(12.dp))

            if (isGuesser) {
                GuesserView(
                    clue = round.clue ?: "",
                    roundIndex = phase.currentRoundIndex,
                    onSubmitGuess = onSubmitGuess
                )
            } else {
                PsychicWaitingView(clue = round.clue ?: "")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuesserView(
    clue: String,
    roundIndex: Int,
    onSubmitGuess: (roundIndex: Int, position: Float) -> Unit
) {
    var guessPosition by remember(roundIndex) { mutableFloatStateOf(0.5f) }

    Text(
        text = "\"$clue\"",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    SpectrumDial(
        guessPosition = guessPosition,
        isDraggable = true,
        onGuessChanged = { guessPosition = it }
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Drag the needle to your guess, then lock it in",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { onSubmitGuess(roundIndex, guessPosition) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Lock In")
    }
}

@Composable
private fun PsychicWaitingView(clue: String) {
    Text(
        text = "Your clue: \"$clue\"",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    SpectrumDial()

    Spacer(modifier = Modifier.height(24.dp))

    CircularProgressIndicator(modifier = Modifier.size(32.dp))

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Waiting for guess...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
