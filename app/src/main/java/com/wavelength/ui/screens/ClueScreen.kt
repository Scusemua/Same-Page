package com.wavelength.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wavelength.game.GameState
import com.wavelength.game.Phase
import com.wavelength.ui.components.SpectrumDial
import com.wavelength.ui.components.SpectrumLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClueScreen(
    gameState: GameState,
    isHost: Boolean,
    onSubmitClue: (roundIndex: Int, clue: String) -> Unit,
    onFinishedClues: () -> Unit,
    onLeaveGame: () -> Unit
) {
    val phase = gameState.phase as? Phase.SubmittingClues ?: return

    val myRounds = gameState.rounds
        .mapIndexed { index, entry -> index to entry }
        .filter { (_, entry) -> entry.psychicIsHost == isHost && entry.card != null }

    val unsubmittedRounds = myRounds.filter { (_, entry) -> entry.clue == null }

    if (unsubmittedRounds.isEmpty() && myRounds.isNotEmpty()) {
        val alreadyMarkedDone = if (isHost) phase.hostDone else phase.guestDone
        LaunchedEffect(alreadyMarkedDone) {
            if (!alreadyMarkedDone) {
                onFinishedClues()
            }
        }
        WaitingForOtherPlayer(onLeaveGame = onLeaveGame)
        return
    }

    val (currentRoundIndex, currentRound) = unsubmittedRounds.first()
    val card = currentRound.card ?: return
    val submittedCount = myRounds.size - unsubmittedRounds.size
    val totalCount = myRounds.size

    var clueText by remember(currentRoundIndex) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Automatically scroll the Submit button into view whenever the keyboard opens
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen) {
            delay(100)
            bringIntoViewRequester.bringIntoView()
        }
    }

    fun submitCurrentClue() {
        if (clueText.isNotBlank()) {
            focusManager.clearFocus()
            onSubmitClue(currentRoundIndex, clueText.trim())
            clueText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Psychic Progress Pill
        Surface(
            shape = CircleShape,
            color = Color(0xFFFF6B35).copy(alpha = 0.14f)
        ) {
            Text(
                text = "PSYCHIC • CLUE ${submittedCount + 1} OF $totalCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SpectrumLabel(leftLabel = card.leftLabel, rightLabel = card.rightLabel)

        Spacer(modifier = Modifier.height(12.dp))

        // Elevated Dial Card with Scoring Zones Visible
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpectrumDial(
                    targetPosition = currentRound.targetPosition,
                    showScoringZones = true,
                    isDraggable = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = clueText,
            onValueChange = { clueText = it },
            label = { Text("Your clue") },
            placeholder = { Text("e.g., Coffee, Submarines...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitCurrentClue() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch {
                            delay(150)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { submitCurrentClue() },
            enabled = clueText.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
        ) {
            Text(
                text = if (submittedCount + 1 == totalCount) "Submit Final Clue" else "Submit Clue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLeaveGame,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Leave Game")
        }
    }
}

@Composable
private fun WaitingForOtherPlayer(onLeaveGame: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaitingPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaitingWave"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = onLeaveGame,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Leave Game")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val primaryColor = Color(0xFFFF6B35)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxRadius = size.minDimension / 2f
                            drawCircle(
                                color = primaryColor.copy(alpha = (1f - pulse) * 0.45f),
                                radius = maxRadius * (0.3f + 0.7f * pulse),
                                style = Stroke(width = 4.dp.toPx())
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 12.dp.toPx()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "All Clues Locked In!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Waiting for the other player to finish writing their clues...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}