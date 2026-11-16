package com.wavelength

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.wavelength.game.Phase
import com.wavelength.net.ConnectionState
import com.wavelength.ui.navigation.Routes
import com.wavelength.ui.navigation.WavelengthNavGraph
import com.wavelength.ui.theme.WavelengthTheme
import com.wavelength.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WavelengthTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WavelengthApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun WavelengthApp(viewModel: GameViewModel) {
    val navController = rememberNavController()
    val gameState by viewModel.gameState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    LaunchedEffect(gameState.phase) {
        val currentRoute = navController.currentDestination?.route
        val targetRoute = when (gameState.phase) {
            is Phase.Lobby -> {
                if (currentRoute != null && currentRoute != Routes.HOME && currentRoute != Routes.LOBBY) {
                    Routes.LOBBY
                } else {
                    null
                }
            }
            is Phase.SubmittingClues -> Routes.CLUE
//            is Phase.Guessing -> Routes.GUESSING
//            is Phase.Reveal -> Routes.REVEAL
            is Phase.Guessing, is Phase.Reveal -> Routes.ROUND
            is Phase.Results -> Routes.RESULTS
        }

        if (targetRoute != null) {
            navController.navigate(targetRoute) {
                launchSingleTop = true
                if (targetRoute == Routes.LOBBY) {
                    popUpTo(Routes.LOBBY) { inclusive = false }
                }
            }
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Disconnected || connectionState is ConnectionState.Error) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != Routes.HOME && currentRoute != Routes.LOBBY) {
                navController.popBackStack(Routes.HOME, inclusive = false)
            }
        }
    }

    WavelengthNavGraph(
        navController = navController,
        viewModel = viewModel
    )
}
