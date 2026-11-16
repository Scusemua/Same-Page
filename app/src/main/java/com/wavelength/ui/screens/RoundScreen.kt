package com.wavelength.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wavelength.game.GameState
import com.wavelength.game.Phase
import com.wavelength.ui.components.SpectrumDial
import com.wavelength.ui.components.SpectrumLabel
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.Switch
import kotlinx.coroutines.launch

@Composable
fun RoundScreen(
    gameState: GameState,
    isHost: Boolean,
    remoteGuessPreview: Float?,
    onSubmitGuess: (roundIndex: Int, position: Float) -> Unit,
    onGuessPreviewChanged: (roundIndex: Int, position: Float) -> Unit,
    onNextRound: () -> Unit,
    onLeaveGame: () -> Unit
) {
    val currentRoundIndex = when (val phase = gameState.phase) {
        is Phase.Guessing -> phase.currentRoundIndex
        is Phase.Reveal -> phase.currentRoundIndex
        else -> return
    }

    val isReveal = gameState.phase is Phase.Reveal
    val round = gameState.rounds.getOrNull(currentRoundIndex) ?: return
    val card = round.card ?: return

    val isGuesser = round.psychicIsHost != isHost
    val roundNumber = currentRoundIndex + 1
    val totalRounds = gameState.rounds.size
    val points = round.pointsAwarded ?: 0

    val haptic = LocalHapticFeedback.current

    // Fans the scoring wedges open and extends the target needle smoothly on reveal
    val revealAnim = remember(currentRoundIndex) {
        Animatable(if (isReveal || !isGuesser) 1f else 0f)
    }
    val pulseAnim = remember(currentRoundIndex) { Animatable(0f) }

    LaunchedEffect(currentRoundIndex, isReveal) {
        if (isReveal) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isGuesser) {
                // Only fan open the wedges for the Guesser, since they were hidden before lock-in
                launch {
                    revealAnim.snapTo(0f)
                    revealAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                    )
                }
            } else {
                // Psychic already sees the wedges—keep them steady at full width
                revealAnim.snapTo(1f)
            }
            launch {
                pulseAnim.snapTo(0f)
                pulseAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
                )
            }
        } else {
            revealAnim.snapTo(if (isGuesser) 0f else 1f)
            pulseAnim.snapTo(0f)
        }
    }

    // Trigger subtle tactile feedback when transitioning into Reveal
    LaunchedEffect(isReveal) {
        if (isReveal) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Keep the guesser's dragged needle position continuous into the reveal phase
    var localGuessPosition by remember(currentRoundIndex) { mutableFloatStateOf(0.5f) }
    var lastPreviewSentAt by remember(currentRoundIndex) { mutableLongStateOf(0L) }
    var showZonesToggle by remember(currentRoundIndex) { mutableStateOf(false) }

    val activeGuessPosition = when {
        isReveal -> round.guessPosition ?: localGuessPosition
        isGuesser -> localGuessPosition
        else -> remoteGuessPreview
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Persistent Top Round Pill
        Surface(
            shape = CircleShape,
            color = Color(0xFFFF6B35).copy(alpha = 0.14f)
        ) {
            AnimatedContent(
                targetState = isReveal,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "RoundBadgePhase"
            ) { revealed ->
                Text(
                    text = if (revealed) {
                        "REVEAL • ROUND $roundNumber OF $totalRounds"
                    } else {
                        "ROUND $roundNumber OF $totalRounds"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent Spectrum Label Card
        SpectrumLabel(leftLabel = card.leftLabel, rightLabel = card.rightLabel)

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent Main Dial & Clue Card
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
                Text(
                    text = if (!isReveal && !isGuesser) "YOUR CLUE" else "CLUE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "\"${round.clue ?: ""}\"",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dial stays mounted in place; wedges fan open via revealProgress
                SpectrumDial(
                    targetPosition = when {
                        isReveal -> round.targetPosition
                        !isGuesser -> if (showZonesToggle) round.targetPosition else null
                        else -> null
                    },
                    guessPosition = activeGuessPosition,
                    showScoringZones = when {
                        isReveal -> true
                        !isGuesser -> showZonesToggle
                        else -> false
                    },
                    revealProgress = revealAnim.value,
                    pulseProgress = pulseAnim.value,
                    isDraggable = !isReveal && isGuesser,
                    onGuessChanged = {
                        localGuessPosition = it
                        val now = System.currentTimeMillis()
                        if (now - lastPreviewSentAt > 80) {
                            lastPreviewSentAt = now
                            onGuessPreviewChanged(currentRoundIndex, it)
                        }
                    }
                )

                // Legend fades/expands in smoothly upon Reveal
                AnimatedVisibility(
                    visible = isReveal,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundLegendDot(color = Color(0xFFD84315), label = "Target")
                            Spacer(modifier = Modifier.width(24.dp))
                            RoundLegendDot(color = Color(0xFF1E88E5), label = "Guess")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Phase Controls: Guessing Controls vs. Reveal Score & Next Round
        AnimatedContent(
            targetState = isReveal,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )) togetherWith fadeOut(tween(180))
            },
            label = "BottomPhaseControls"
        ) { revealed ->
            if (revealed) {
                val scoreAccentColor = when {
                    points >= 4 -> Color(0xFFFF6B35)
                    points == 3 -> Color(0xFFFF8C42)
                    points == 2 -> Color(0xFFFFA366)
                    else -> MaterialTheme.colorScheme.error
                }
                val verdictText = when {
                    points >= 4 -> "Bullseye!"
                    points == 3 -> "Nice!"
                    points == 2 -> "Close!"
                    else -> "Miss!"
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = scoreAccentColor.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "+$points",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreAccentColor
                            )
                            Text(
                                text = verdictText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scoreAccentColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Total Score: ${gameState.totalScore}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (isHost) {
                        Button(
                            onClick = onNextRound,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B35),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = if (roundNumber < totalRounds) "Next Round" else "See Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Waiting for host to continue...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            } else {
// Guessing Phase Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isGuesser) {
                        Text(
                            text = "Tap or drag the needle to your guess, then lock it in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onSubmitGuess(currentRoundIndex, localGuessPosition) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B35),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Lock In Guess",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFFFF6B35)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Waiting for the other player to lock in their guess...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = showZonesToggle,
                                onCheckedChange = { showZonesToggle = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Show target zones",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Total Score: ${gameState.totalScore}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLeaveGame,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Leave Game")
        }
    }
}
@Composable
private fun RoundLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}