package com.wavelength.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wavelength.game.GameState
import com.wavelength.game.Phase
import com.wavelength.ui.components.SpectrumDial
import com.wavelength.ui.components.SpectrumLabel

@Composable
fun RevealScreen(
    gameState: GameState,
    isHost: Boolean,
    onNextRound: () -> Unit,
    onLeaveGame: () -> Unit
) {
    val phase = gameState.phase as? Phase.Reveal ?: return
    val round = gameState.rounds.getOrNull(phase.currentRoundIndex) ?: return
    val card = round.card ?: return
    val points = round.pointsAwarded ?: 0
    val roundNumber = phase.currentRoundIndex + 1
    val totalRounds = gameState.rounds.size

    // Subtle pop-in spring animation for the score badge
    var revealed by remember(phase.currentRoundIndex) { mutableStateOf(false) }
    LaunchedEffect(phase.currentRoundIndex) {
        revealed = true
    }
    val scoreScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ScoreRevealScale"
    )

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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Round Pill
        Surface(
            shape = CircleShape,
            color = Color(0xFFFF6B35).copy(alpha = 0.14f)
        ) {
            Text(
                text = "REVEAL • ROUND $roundNumber OF $totalRounds",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SpectrumLabel(leftLabel = card.leftLabel, rightLabel = card.rightLabel)

        Spacer(modifier = Modifier.height(12.dp))

        // Main Dial & Clue Card
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
                    text = "CLUE",
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

                SpectrumDial(
                    targetPosition = round.targetPosition,
                    guessPosition = round.guessPosition,
                    showScoringZones = true,
                    isDraggable = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Needle Legend (Target vs Guess)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = Color(0xFFFF6B35), label = "Target")
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendDot(color = Color(0xFF1E88E5), label = "Guess")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score & Verdict Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scoreScale),
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

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onLeaveGame,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Leave Game")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
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
