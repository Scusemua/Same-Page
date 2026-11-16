package com.wavelength.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wavelength.net.ConnectionRole
import com.wavelength.net.NearbyPermissions
import com.wavelength.ui.screens.ClueScreen
import com.wavelength.ui.screens.GuessingScreen
import com.wavelength.ui.screens.HomeScreen
import com.wavelength.ui.screens.LobbyScreen
import com.wavelength.ui.screens.ResultsScreen
import com.wavelength.ui.screens.RevealScreen
import com.wavelength.ui.screens.RoundScreen
import com.wavelength.ui.viewmodel.GameViewModel

object Routes {
    const val HOME = "home"
    const val LOBBY = "lobby"
    const val CLUE = "clue"
    const val GUESSING = "guessing"
    const val REVEAL = "reveal"
    const val ROUND = "round"
    const val RESULTS = "results"
}

@Composable
fun WavelengthNavGraph(
    navController: NavHostController,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pendingAction?.invoke()
        } else {
            Toast.makeText(
                context,
                "Nearby devices permission is required to host or join a game.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingAction = null
    }

    fun runWithPermissions(action: () -> Unit) {
        if (NearbyPermissions.hasAllPermissions(context)) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(NearbyPermissions.requiredPermissions)
        }
    }

    fun leaveToHome() {
        viewModel.disconnect()
        navController.popBackStack(Routes.HOME, inclusive = false)
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 12 },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 12 },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 12 },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth / 12 },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    )
        }
    ) {
        composable(Routes.HOME) {
            val useFake by viewModel.useFakeConnection.collectAsState()
            HomeScreen(
                onHostGame = {
                    runWithPermissions {
                        viewModel.hostGame()
                        navController.navigate(Routes.LOBBY) {
                            popUpTo(Routes.HOME)
                        }
                    }
                },
                onJoinGame = {
                    runWithPermissions {
                        viewModel.joinGame()
                        navController.navigate(Routes.LOBBY) {
                            popUpTo(Routes.HOME)
                        }
                    }
                },
                useFakeConnection = useFake,
                onToggleFakeConnection = { viewModel.setUseFakeConnection(it) }
            )
        }

        composable(Routes.LOBBY) {
            BackHandler { leaveToHome() }
            LobbyScreen(
                connectionState = connectionState,
                isHost = viewModel.localRole == ConnectionRole.HOST,
                onStartGame = { viewModel.startGame() },
                onBack = { leaveToHome() }
            )
        }

        composable(Routes.CLUE) {
            BackHandler { leaveToHome() }
            ClueScreen(
                gameState = gameState,
                isHost = viewModel.localRole == ConnectionRole.HOST,
                onSubmitClue = { roundIndex, clue ->
                    viewModel.submitClue(roundIndex, clue)
                },
                onFinishedClues = { viewModel.finishClues() },
                onLeaveGame = { leaveToHome() }
            )
        }

        composable(Routes.GUESSING) {
            BackHandler { leaveToHome() }
            GuessingScreen(
                gameState = gameState,
                isHost = viewModel.localRole == ConnectionRole.HOST,
                onSubmitGuess = { roundIndex, position ->
                    viewModel.submitGuess(roundIndex, position)
                },
                onLeaveGame = { leaveToHome() }
            )
        }

        composable(Routes.REVEAL) {
            BackHandler { leaveToHome() }
            RevealScreen(
                gameState = gameState,
                isHost = viewModel.localRole == ConnectionRole.HOST,
                onNextRound = { viewModel.advanceToNextRound() },
                onLeaveGame = { leaveToHome() }
            )
        }

        composable(Routes.RESULTS) {
            BackHandler { leaveToHome() }
            ResultsScreen(
                gameState = gameState,
                onPlayAgain = { viewModel.playAgain() },
                onQuit = { leaveToHome() }
            )
        }

        composable(Routes.ROUND) {
            BackHandler { leaveToHome() }
            RoundScreen(
                gameState = gameState,
                isHost = viewModel.localRole == ConnectionRole.HOST,
                onSubmitGuess = { roundIndex, position ->
                    viewModel.submitGuess(roundIndex, position)
                },
                onNextRound = { viewModel.advanceToNextRound() },
                onLeaveGame = { leaveToHome() }
            )
        }
    }
}
